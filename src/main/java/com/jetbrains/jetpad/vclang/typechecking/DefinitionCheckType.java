package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeOmega;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
import com.jetbrains.jetpad.vclang.core.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.*;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.CompositeInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.LocalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CollectDefCallsVisitor;
import com.jetbrains.jetpad.vclang.typechecking.visitor.FindMatchOnIntervalVisitor;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.size;
import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.core.pattern.Utils.processImplicit;
import static com.jetbrains.jetpad.vclang.core.pattern.Utils.toPatterns;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

public class DefinitionCheckType {
  public static Definition typeCheckHeader(CheckTypeVisitor visitor, GlobalInstancePool instancePool, Abstract.Definition definition, Abstract.ClassDefinition enclosingClass) {
    LocalInstancePool localInstancePool = new LocalInstancePool();
    visitor.setClassViewInstancePool(new CompositeInstancePool(localInstancePool, instancePool));
    ClassDefinition typedEnclosingClass = enclosingClass == null ? null : (ClassDefinition) visitor.getTypecheckingState().getTypechecked(enclosingClass);

    if (definition instanceof Abstract.FunctionDefinition) {
      return typeCheckFunctionHeader((Abstract.FunctionDefinition) definition, typedEnclosingClass, visitor, localInstancePool);
    } else
    if (definition instanceof Abstract.DataDefinition) {
      return typeCheckDataHeader((Abstract.DataDefinition) definition, typedEnclosingClass, visitor, localInstancePool);
    } else {
      throw new IllegalStateException();
    }
  }

  public static void typeCheckBody(Definition definition, CheckTypeVisitor exprVisitor) {
    if (definition instanceof FunctionDefinition) {
      typeCheckFunctionBody((FunctionDefinition) definition, exprVisitor);
    } else
    if (definition instanceof DataDefinition) {
      typeCheckDataBody((DataDefinition) definition, exprVisitor);
    } else {
      throw new IllegalStateException();
    }
  }

  public static Definition typeCheck(TypecheckerState state, GlobalInstancePool instancePool, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, TypecheckingUnit unit, LocalErrorReporter errorReporter) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(state, staticNsProvider, dynamicNsProvider, null, null, new ArrayList<Binding>(), new ArrayList<LevelBinding>(), errorReporter, instancePool);
    ClassDefinition enclosingClass = unit.getEnclosingClass() == null ? null : (ClassDefinition) state.getTypechecked(unit.getEnclosingClass());

    if (unit.getDefinition() instanceof Abstract.ClassDefinition) {
      return typeCheckClass((Abstract.ClassDefinition) unit.getDefinition(), enclosingClass, visitor);
    } else
    if (unit.getDefinition() instanceof Abstract.ClassViewInstance) {
      return typeCheckClassViewInstance((Abstract.ClassViewInstance) unit.getDefinition(), visitor);
    }

    LocalInstancePool localInstancePool = new LocalInstancePool();
    visitor.setClassViewInstancePool(new CompositeInstancePool(localInstancePool, instancePool));

    if (unit.getDefinition() instanceof Abstract.FunctionDefinition) {
      FunctionDefinition definition = typeCheckFunctionHeader((Abstract.FunctionDefinition) unit.getDefinition(), enclosingClass, visitor, localInstancePool);
      if (definition.hasErrors() == Definition.TypeCheckingStatus.TYPE_CHECKING) {
        typeCheckFunctionBody(definition, visitor);
      }
      return definition;
    } else
    if (unit.getDefinition() instanceof Abstract.DataDefinition) {
      DataDefinition definition = typeCheckDataHeader((Abstract.DataDefinition) unit.getDefinition(), enclosingClass, visitor, localInstancePool);
      if (definition.hasErrors() == Definition.TypeCheckingStatus.TYPE_CHECKING) {
        typeCheckDataBody(definition, visitor);
      }
      return definition;
    } else {
      throw new IllegalStateException();
    }
  }

  private static DependentLink createThisParam(ClassDefinition enclosingClass, LevelArguments polyArgs) {
    assert enclosingClass != null;
    return param("\\this", ClassCall(enclosingClass, polyArgs));
  }

  private static boolean isPolyParam(Abstract.TypeArgument arg) {
    return arg.getType() instanceof Abstract.LvlExpression;
  }

  private static List<LevelBinding> typeCheckPolyParam(Abstract.TypeArgument typeArgument, Abstract.SourceNode node, LocalErrorReporter errorReporter) {
    assert (typeArgument.getType() instanceof Abstract.LvlExpression);
    if (!(typeArgument instanceof Abstract.TelescopeArgument)) {
      errorReporter.report(new LocalTypeCheckingError("Parameter of type Lvl must have name", node));
      return null;
    }
    Abstract.TelescopeArgument teleArgument = (Abstract.TelescopeArgument)typeArgument;
    if (teleArgument.getExplicit()) {
      errorReporter.report(new LocalTypeCheckingError("Polymorphic variables must be implicit", node));
      return null;
    }
    List<LevelBinding> levels = new ArrayList<>();
    for (String name : teleArgument.getNames()) {
      levels.add(new LevelBinding(name, LevelVariable.LvlType.PLVL));
    }
    return levels;
  }

  private static boolean typeCheckParameters(List<? extends Abstract.Argument> arguments, Abstract.SourceNode node, List<Binding> context, List<LevelBinding> lvlContext, List<LevelBinding> polyParamsList, LinkList list, CheckTypeVisitor visitor, LocalInstancePool localInstancePool, Map<Integer, ClassField> classifyingFields) {
    boolean ok = true;
    boolean polyParamsAllowed = true;
    List<LevelBinding> genPolyParams = new ArrayList<>();
    int index = 0;

    genPolyParams.add(null);
    genPolyParams.add(null);

    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument)argument;

        if (isPolyParam(typeArgument)) {
          if (!polyParamsAllowed) {
            visitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.ERROR, "Poly parameters must be declared in the beginning of a definition", argument));
            ok = false;
            continue;
          }
          List<LevelBinding> levelTeleParam = typeCheckPolyParam(typeArgument, node, visitor.getErrorReporter());
          if (levelTeleParam == null) {
            ok = false;
            continue;
          }
          lvlContext.addAll(levelTeleParam);
          polyParamsList.addAll(levelTeleParam);
          ++index;
          continue;
        } else {
          polyParamsAllowed = false;
        }

        Type paramType = visitor.checkParamType(typeArgument.getType(), genPolyParams);
        if (paramType == null) {
          ok = false;
          continue;
        }

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
          param = param(argument.getExplicit(), names, paramType);
          index += names.size();
        } else {
          param = param(argument.getExplicit(), (String) null, paramType);
          index++;
        }

        if (classifyingFields != null && localInstancePool != null) {
          Abstract.ClassView classView = Abstract.getUnderlyingClassView(typeArgument.getType());
          if (classView != null && classView.getClassifyingField() != null) {
            ClassField classifyingField = (ClassField) visitor.getTypecheckingState().getTypechecked(classView.getClassifyingField());
            classifyingFields.put(index - 1, classifyingField);
            for (DependentLink link = param; link.hasNext(); link = link.getNext()) {
              ReferenceExpression reference = new ReferenceExpression(link);
              if (!localInstancePool.addInstance(FieldCall(classifyingField, reference).normalize(NormalizeVisitor.Mode.NF), classView, reference)) {
                visitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "Duplicate instance", argument)); // FIXME[error] better error message
              }
            }
          }
        }

        list.append(param);
        context.addAll(toContext(param));
      } else {
        visitor.getErrorReporter().report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0]));
        ok = false;
      }
    }

    if (genPolyParams.get(0) != null) {
      polyParamsList.add(genPolyParams.get(0));
    }
    if (genPolyParams.get(1) != null) {
      polyParamsList.add(genPolyParams.get(1));
    }
    return ok;
  }

  private static FunctionDefinition typeCheckFunctionHeader(Abstract.FunctionDefinition def, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    List<LevelBinding> polyParamsList = new ArrayList<>();
    LinkList list = new LinkList();
    if (enclosingClass != null) {
      for (LevelBinding param : enclosingClass.getPolyParams()) {
        polyParamsList.add(new LevelBinding(param.getName(), param.getType()));
      }
      DependentLink thisParam = createThisParam(enclosingClass, new LevelArguments(Level.map(polyParamsList)));
      visitor.getContext().add(thisParam);
      visitor.getLevelContext().addAll(enclosingClass.getPolyParams());
      list.append(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
    }

    Map<Integer, ClassField> classifyingFields = new HashMap<>();
    boolean paramsOk = typeCheckParameters(def.getArguments(), def, visitor.getContext(), visitor.getLevelContext(), polyParamsList, list, visitor, localInstancePool, classifyingFields);
    TypeMax expectedType = null;
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      expectedType = visitor.checkFunOrDataType(resultType);
    }

    FunctionDefinition typedDef = new FunctionDefinition(def, list.getFirst(), expectedType, null);
    visitor.getTypecheckingState().record(def, typedDef);
    typedDef.setClassifyingFieldsOfParameters(classifyingFields);
    typedDef.setThisClass(enclosingClass);
    typedDef.setPolyParams(polyParamsList);
    // TODO: We should remember whether there were errors in parameters.
    typedDef.typeHasErrors(!paramsOk || expectedType == null);
    typedDef.hasErrors(paramsOk ? Definition.TypeCheckingStatus.TYPE_CHECKING : Definition.TypeCheckingStatus.HAS_ERRORS);
    return typedDef;
  }

  private static void typeCheckFunctionBody(FunctionDefinition typedDef, CheckTypeVisitor visitor) {
    Abstract.FunctionDefinition def = (Abstract.FunctionDefinition) typedDef.getAbstractDefinition();

    TypeMax userType = typedDef.getResultType();
    Type expectedType = null; //userType == null ? null : userType instanceof Type ? (Type) userType : TypeOmega.getInstance();

    if (userType != null) {
      if (userType instanceof Type) {
        expectedType = (Type) userType;
      } else {
        Level expPlevel = userType.getPiCodomain().toSorts().getPLevel().toLevel();
        Level expHlevel = userType.getPiCodomain().toSorts().getHLevel().toLevel();

        if (expPlevel == null || expPlevel.isInfinity()) {
          InferenceLevelVariable lpVar = new InferenceLevelVariable("\\expLP", LevelVariable.LvlType.PLVL, def);
          visitor.getEquations().addVariable(lpVar);
          expPlevel = new Level(lpVar);
        }
        if (expHlevel == null || expHlevel.isInfinity()) {
          InferenceLevelVariable lhVar = new InferenceLevelVariable("\\expLH", LevelVariable.LvlType.HLVL, def);
          visitor.getEquations().addVariable(lhVar);
          expHlevel = new Level(lhVar);
        }
        UniverseExpression universe = Universe(expPlevel, expHlevel);
        expectedType = userType.getPiParameters().hasNext() ? Pi(userType.getPiParameters(), universe) : universe;
        // TODO: result type should be set to expectedType at this point
        typedDef.setResultType(Universe(Sort.PROP));
      }
    }

    Abstract.Expression term = def.getTerm();
    TypeMax actualType = null;
    if (term != null) {
      if (term instanceof Abstract.ElimExpression) {
        visitor.getContext().subList(visitor.getContext().size() - size(typedDef.getParameters()), visitor.getContext().size()).clear();
        ElimTreeNode elimTree = visitor.getTypeCheckingElim().typeCheckElim((Abstract.ElimExpression) term, def.getArrow() == Abstract.Definition.Arrow.LEFT ? typedDef.getParameters() : null, expectedType, false, true);
        if (elimTree != null) {
          typedDef.setElimTree(elimTree);
          if (userType != null && !(userType instanceof Type)) {
            final SortMax sorts = new SortMax();
            elimTree.accept(new ElimTreeNodeVisitor<Void, Void>() {
              @Override
              public Void visitBranch(BranchElimTreeNode branchNode, Void params) {
                for (ConstructorClause clause : branchNode.getConstructorClauses()) {
                  clause.getChild().accept(this, null);
                }
                if (branchNode.getOtherwiseClause() != null) {
                  branchNode.getOtherwiseClause().getChild().accept(this, null);
                }
                return null;
              }

              @Override
              public Void visitLeaf(LeafElimTreeNode leafNode, Void params) {
                SortMax sorts1 = leafNode.getExpression().getType().getPiCodomain().toSorts();
                if (sorts1 != null) {
                  sorts.add(sorts1);
                }
                return null;
              }

              @Override
              public Void visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
                return null;
              }
            }, null);
            actualType = new PiUniverseType(userType.getPiParameters(), sorts);
          } else {
            actualType = userType;
          }
        }
      } else {
        CheckTypeVisitor.Result termResult = visitor.checkType(term, expectedType);
        if (termResult != null) {
          typedDef.setElimTree(top(typedDef.getParameters(), leaf(def.getArrow(), termResult.expression)));
          actualType = termResult.type;
        }
      }

      if (actualType != null && !(actualType instanceof ErrorExpression)) {
        typedDef.typeHasErrors(false);
        typedDef.setResultType(actualType);
        if (userType != null && !(userType instanceof Type)) {
          SortMax actualSorts = actualType.getPiCodomain().toSorts();
          if (actualSorts == null || !actualSorts.isLessOrEquals(userType.getPiCodomain().toSorts())) {
            visitor.getErrorReporter().report(new TypeMismatchError(userType, actualType, term));
          }
        }
      }

      typedDef.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);

      if (typedDef.getElimTree() != null) {
        LocalTypeCheckingError error = TypeCheckingElim.checkCoverage(def, typedDef.getParameters(), typedDef.getElimTree(), expectedType);
        if (error != null) {
          visitor.getErrorReporter().report(error);
        }
      }

      if (typedDef.getElimTree() != null) {
        LocalTypeCheckingError error = TypeCheckingElim.checkConditions(def, typedDef.getParameters(), typedDef.getElimTree());
        if (error != null) {
          visitor.getErrorReporter().report(error);
          typedDef.setElimTree(null);
        }
      }
    }

    if (typedDef.getResultType() == null) {
      typedDef.typeHasErrors(true);
      typedDef.hasErrors(Definition.TypeCheckingStatus.HAS_ERRORS);
    } else {
      typedDef.hasErrors(typedDef.getElimTree() != null ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.HAS_ERRORS);
    }
  }

  private static DataDefinition typeCheckDataHeader(Abstract.DataDefinition def, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    LinkList list = new LinkList();
    List<LevelBinding> polyParamsList = new ArrayList<>();
    if (enclosingClass != null) {
      for (LevelBinding param : enclosingClass.getPolyParams()) {
        polyParamsList.add(new LevelBinding(param.getName(), param.getType()));
      }
      DependentLink thisParam = createThisParam(enclosingClass, new LevelArguments(Level.map(polyParamsList)));
      visitor.getContext().add(thisParam);
      visitor.getLevelContext().addAll(polyParamsList);
      list.append(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
    }

    Map<Integer, ClassField> classifyingFields = new HashMap<>();
    SortMax userSorts = null;
    boolean paramsOk;
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
      paramsOk = typeCheckParameters(def.getParameters(), def, visitor.getContext(), visitor.getLevelContext(), polyParamsList, list, visitor, localInstancePool, classifyingFields);

      if (def.getUniverse() != null) {
        if (def.getUniverse() instanceof Abstract.PolyUniverseExpression) {
          TypeMax userType = visitor.checkFunOrDataType(def.getUniverse());
          userSorts = userType == null ? SortMax.OMEGA : userType.toSorts();
        } else {
          String msg = "Specified type " + PrettyPrintVisitor.prettyPrint(def.getUniverse(), 0) + " of '" + def.getName() + "' is not a universe";
          visitor.getErrorReporter().report(new LocalTypeCheckingError(msg, def.getUniverse()));
        }
      }
    }
    if (userSorts == null) {
      userSorts = SortMax.OMEGA;
    }

    DataDefinition dataDefinition = new DataDefinition(def, userSorts, list.getFirst());
    dataDefinition.setClassifyingFieldsOfParameters(classifyingFields);
    dataDefinition.setThisClass(enclosingClass);
    dataDefinition.setPolyParams(polyParamsList);
    visitor.getTypecheckingState().record(def, dataDefinition);

    if (!paramsOk) {
      dataDefinition.typeHasErrors(true);
      dataDefinition.hasErrors(Definition.TypeCheckingStatus.HAS_ERRORS);
      for (Abstract.Constructor constructor : def.getConstructors()) {
        visitor.getTypecheckingState().record(constructor, new Constructor(constructor, dataDefinition));
      }
      return dataDefinition;
    }
    dataDefinition.typeHasErrors(false);
    dataDefinition.hasErrors(Definition.TypeCheckingStatus.TYPE_CHECKING);
    return dataDefinition;
  }

  private static void typeCheckDataBody(DataDefinition dataDefinition, CheckTypeVisitor visitor) {
    Abstract.DataDefinition def = dataDefinition.getAbstractDefinition();
    SortMax userSorts = def.getUniverse() != null ? dataDefinition.getSorts() : null;
    SortMax inferredSorts = def.getConstructors().size() > 1 ? new SortMax(Sort.SET0) : new SortMax();
    dataDefinition.setSorts(inferredSorts);

    boolean dataOk = true;
    boolean universeOk = userSorts == null || userSorts.getPLevel() != null;
    for (Abstract.Constructor constructor : def.getConstructors()) {
      visitor.getContext().clear();
      SortMax conSorts = new SortMax();
      Constructor typedConstructor = typeCheckConstructor(constructor, dataDefinition, visitor, conSorts);
      visitor.getTypecheckingState().record(constructor, typedConstructor);
      if (typedConstructor.typeHasErrors()) {
        dataOk = false;
      }

      inferredSorts.add(conSorts);
      if (userSorts != null) {
        if (!conSorts.isLessOrEquals(userSorts)) {
          String msg = "Universe " + conSorts + " of constructor '" + constructor.getName() + "' is not compatible with expected universe " + userSorts;
          visitor.getErrorReporter().report(new LocalTypeCheckingError(msg, constructor));
          universeOk = false;
        }
      }
    }
    dataDefinition.hasErrors(dataOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.HAS_ERRORS);

    visitor.getContext().clear();
    if (def.getConditions() != null) {
      List<Constructor> cycle = typeCheckConditions(visitor, dataDefinition, def);
      if (cycle != null) {
        StringBuilder cycleConditionsError = new StringBuilder();
        cycleConditionsError.append("Conditions form a cycle: ");
        for (Constructor constructor : cycle) {
          cycleConditionsError.append(constructor.getName()).append(" - ");
        }
        cycleConditionsError.append(cycle.get(0).getName());
        LocalTypeCheckingError error = new LocalTypeCheckingError(cycleConditionsError.toString(), def);
        visitor.getErrorReporter().report(error);
      }
    }

    if (!dataDefinition.getConditions().isEmpty()) {
      List<Condition> failedConditions = new ArrayList<>();
      for (Condition condition : dataDefinition.getConditions()) {
        LocalTypeCheckingError error = TypeCheckingElim.checkConditions(condition.getConstructor().getName(), def, condition.getConstructor().getParameters(), condition.getElimTree());
        if (error != null) {
          visitor.getErrorReporter().report(error);
          failedConditions.add(condition);
          dataDefinition.hasErrors(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
      }
      dataDefinition.getConditions().removeAll(failedConditions);

      for (Condition condition : dataDefinition.getConditions()) {
        if (condition.getElimTree().accept(new FindMatchOnIntervalVisitor(), null)) {
          dataDefinition.setMatchesOnInterval();
          inferredSorts = new SortMax(inferredSorts.getPLevel(), LevelMax.INFINITY);
          break;
        }
      }
    }

    if (universeOk && userSorts != null) {
      boolean shouldBeTruncated = !inferredSorts.isLessOrEquals(userSorts);
      if (!shouldBeTruncated || def.isTruncated()) {
        if (!userSorts.isOmega()) {
          inferredSorts = userSorts;
          dataDefinition.isTruncated(shouldBeTruncated);
        }
      } else {
        String msg = "Actual universe " + inferredSorts + " is not compatible with expected universe " + userSorts;
        visitor.getErrorReporter().report(new LocalTypeCheckingError(msg, def.getUniverse()));
      }
    }

    dataDefinition.setSorts(inferredSorts);
  }

  private static List<Constructor> typeCheckConditions(CheckTypeVisitor visitor, DataDefinition dataDefinition, Abstract.DataDefinition def) {
    Map<Constructor, List<Abstract.Condition>> condMap = new HashMap<>();
    for (Abstract.Condition cond : def.getConditions()) {
      Constructor constructor = dataDefinition.getConstructor(cond.getConstructorName());
      if (constructor == null) {
        visitor.getErrorReporter().report(new NotInScopeError(def, cond.getConstructorName()));  // TODO: refer by reference
        continue;
      }
      if (constructor.typeHasErrors()) {
        continue;
      }
      if (!condMap.containsKey(constructor)) {
        condMap.put(constructor, new ArrayList<Abstract.Condition>());
      }
      condMap.get(constructor).add(cond);
    }
    List<Constructor> cycle = searchConditionCycle(condMap, visitor.getTypecheckingState());
    if (cycle != null) {
      return cycle;
    }
    for (Constructor constructor : condMap.keySet()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
        List<List<Pattern>> patterns = new ArrayList<>();
        List<Expression> expressions = new ArrayList<>();
        List<Abstract.Definition.Arrow> arrows = new ArrayList<>();
        visitor.getContext().addAll(toContext(constructor.getDataTypeParameters()));

        for (Abstract.Condition cond : condMap.get(constructor)) {
          try (Utils.ContextSaver saver = new Utils.ContextSaver(visitor.getContext())) {
            List<Expression> resultType = new ArrayList<>(Collections.singletonList(constructor.getDataTypeExpression(null)));
            DependentLink params = constructor.getParameters();
            List<Abstract.PatternArgument> processedPatterns = processImplicitPatterns(cond, params, cond.getPatterns(), visitor.getErrorReporter());
            if (processedPatterns == null)
              continue;

            Patterns typedPatterns = visitor.getTypeCheckingElim().visitPatternArgs(processedPatterns, constructor.getParameters(), resultType, TypeCheckingElim.PatternExpansionMode.CONDITION);
            if (typedPatterns == null) {
              continue;
            }

            CheckTypeVisitor.Result result = visitor.checkType(cond.getTerm(), resultType.get(0));
            if (result == null)
              continue;

            patterns.add(toPatterns(typedPatterns.getPatterns()));
            expressions.add(result.expression.normalize(NormalizeVisitor.Mode.NF));
            arrows.add(Abstract.Definition.Arrow.RIGHT);
          }
        }

        PatternsToElimTreeConversion.OKResult elimTreeResult = (PatternsToElimTreeConversion.OKResult) PatternsToElimTreeConversion.convert(constructor.getParameters(), patterns, expressions, arrows);

        if (!elimTreeResult.elimTree.accept(new TerminationCheckVisitor(constructor, constructor.getDataTypeParameters(), constructor.getParameters()), null)) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Termination check failed", null));
          continue;
        }

        Condition typedCond = new Condition(constructor, elimTreeResult.elimTree);
        dataDefinition.addCondition(typedCond);
        for (Abstract.Condition cond : condMap.get(constructor)) {
          cond.setWellTyped(typedCond);
        }
      }
    }
    return null;
  }

  private static List<Constructor> searchConditionCycle(Map<Constructor, List<Abstract.Condition>> condMap, TypecheckerState state) {
    Set<Constructor> visited = new HashSet<>();
    List<Constructor> visiting = new ArrayList<>();
    for (Constructor constructor : condMap.keySet()) {
      List<Constructor> cycle = searchConditionCycle(condMap, constructor, visited, visiting, state);
      if (cycle != null)
        return cycle;
    }
    return null;
  }

  private static List<Constructor> searchConditionCycle(Map<Constructor, List<Abstract.Condition>> condMap, Constructor constructor, Set<Constructor> visited, List<Constructor> visiting, TypecheckerState state) {
    if (visited.contains(constructor))
      return null;
    if (visiting.contains(constructor)) {
      return visiting.subList(visiting.lastIndexOf(constructor), visiting.size());
    }
    visiting.add(constructor);
    if (condMap.containsKey(constructor)) {
      for (Abstract.Condition condition : condMap.get(constructor)) {
        Set<Abstract.Definition> dependencies = new HashSet<>();
        condition.getTerm().accept(new CollectDefCallsVisitor(null, dependencies), null);
        for (Abstract.Definition def : dependencies) {
          Definition typeCheckedDef = state.getTypechecked(def);
          if (typeCheckedDef != null && typeCheckedDef != constructor && typeCheckedDef instanceof Constructor && ((Constructor) typeCheckedDef).getDataType().equals(constructor.getDataType())) {
            List<Constructor> cycle = searchConditionCycle(condMap, (Constructor) typeCheckedDef, visited, visiting, state);
            if (cycle != null)
              return cycle;
          }
        }
      }
    }
    visiting.remove(visiting.size() - 1);
    visited.add(constructor);
    return null;
  }

  private static Constructor typeCheckConstructor(Abstract.Constructor def, DataDefinition dataDefinition, CheckTypeVisitor visitor, SortMax sorts) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(visitor.getContext())) {
      List<? extends Abstract.TypeArgument> arguments = def.getArguments();
      String name = def.getName();

      Constructor constructor = new Constructor(def, null, dataDefinition, null);
      List<? extends Abstract.PatternArgument> patterns = def.getPatterns();
      Patterns typedPatterns = null;
      if (patterns != null) {
        List<Abstract.PatternArgument> processedPatterns = new ArrayList<>(patterns);
        if (dataDefinition.getThisClass() != null) {
          processedPatterns.add(0, new PatternArgument(new NamePattern(dataDefinition.getParameters()), true, true));
        }
        processedPatterns = processImplicitPatterns(def, dataDefinition.getParameters(), processedPatterns, visitor.getErrorReporter());
        if (processedPatterns == null) {
          return constructor;
        }

        typedPatterns = visitor.getTypeCheckingElim().visitPatternArgs(processedPatterns, dataDefinition.getParameters(), Collections.<Expression>emptyList(), TypeCheckingElim.PatternExpansionMode.DATATYPE);
        if (typedPatterns == null) {
          return constructor;
        }
      } else {
        visitor.getContext().addAll(toContext(dataDefinition.getParameters()));
      }

      if (dataDefinition.getThisClass() != null && typedPatterns != null) {
        visitor.setThisClass(dataDefinition.getThisClass(), Reference(typedPatterns.getParameters()));
      }

      LinkList list = new LinkList();
      for (Abstract.TypeArgument argument : arguments) {
        CheckTypeVisitor.Result paramResult = visitor.checkType(argument.getType(), TypeOmega.getInstance());
        if (paramResult == null) {
          return constructor;
        }

        sorts.add(paramResult.type.toSorts());

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          param = param(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), paramResult.expression);
        } else {
          param = param(argument.getExplicit(), (String) null, paramResult.expression);
        }
        list.append(param);
        visitor.getContext().addAll(toContext(param));
      }

      for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext()) {
        Type type = link.getType().normalize(NormalizeVisitor.Mode.WHNF);
        List<DependentLink> piParams = new ArrayList<>();
        type = type.getPiParameters(piParams, true, false);
        for (DependentLink piParam : piParams) {
          if (piParam instanceof UntypedDependentLink) {
            continue;
          }
          if (!checkNonPositiveError(piParam.getType().toExpression(), dataDefinition, name, list.getFirst(), link, arguments, def, visitor.getErrorReporter())) {
            return constructor;
          }
        }

        boolean check = true;
        while (check) {
          check = false;
          if (type.toExpression() != null) {
            if (type.toExpression().toDataCall() != null) {
              List<? extends Expression> exprs = type.toExpression().toDataCall().getDefCallArguments();
              DataDefinition typeDef = type.toExpression().toDataCall().getDefinition();
              if (typeDef == Prelude.PATH && exprs.size() >= 1) {
                LamExpression lam = exprs.get(0).normalize(NormalizeVisitor.Mode.WHNF).toLam();
                if (lam != null) {
                  check = true;
                  type = lam.getBody().normalize(NormalizeVisitor.Mode.WHNF);
                  exprs = exprs.subList(1, exprs.size());
                }
              }

              for (Expression expr : exprs) {
                if (!checkNonPositiveError(expr, dataDefinition, name, list.getFirst(), link, arguments, def, visitor.getErrorReporter())) {
                  return constructor;
                }
              }
            } else {
              if (!checkNonPositiveError(type.toExpression(), dataDefinition, name, list.getFirst(), link, arguments, def, visitor.getErrorReporter())) {
                return constructor;
              }
            }
          }
        }
      }

      constructor.setParameters(list.getFirst());
      constructor.setPatterns(typedPatterns);
      constructor.typeHasErrors(false);
      constructor.setThisClass(dataDefinition.getThisClass());
      dataDefinition.addConstructor(constructor);

      visitor.getTypecheckingState().record(def, constructor);
      return constructor;
    }
  }

  private static boolean checkNonPositiveError(Expression expr, DataDefinition dataDefinition, String name, DependentLink params, DependentLink param, List<? extends Abstract.Argument> args, Abstract.Constructor constructor, LocalErrorReporter errorReporter) {
    if (!expr.findBinding(dataDefinition)) {
      return true;
    }

    int index = DependentLink.Helper.getIndex(params, param);
    int i = 0;
    Abstract.Argument argument = null;
    for (Abstract.Argument arg : args) {
      if (arg instanceof Abstract.TelescopeArgument) {
        i += ((Abstract.TelescopeArgument) arg).getNames().size();
      } else {
        i++;
      }
      if (i > index) {
        argument = arg;
        break;
      }
    }

    String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + name;
    errorReporter.report(new LocalTypeCheckingError(msg, argument == null ? constructor : argument));
    return false;
  }

  private static List<Abstract.PatternArgument> processImplicitPatterns(Abstract.SourceNode expression, DependentLink parameters, List<? extends Abstract.PatternArgument> patterns, LocalErrorReporter errorReporter) {
    List<Abstract.PatternArgument> processedPatterns = null;
    ProcessImplicitResult processImplicitResult = processImplicit(patterns, parameters);
    if (processImplicitResult.patterns == null) {
      if (processImplicitResult.numExcessive != 0) {
        errorReporter.report(new LocalTypeCheckingError("Too many arguments: " + processImplicitResult.numExcessive + " excessive", expression));
      } else if (processImplicitResult.wrongImplicitPosition < patterns.size()) {
        errorReporter.report(new LocalTypeCheckingError("Unexpected implicit argument", patterns.get(processImplicitResult.wrongImplicitPosition)));
      } else {
        errorReporter.report(new LocalTypeCheckingError("Too few explicit arguments, expected: " + processImplicitResult.numExplicit, expression));
      }
    } else {
      processedPatterns = processImplicitResult.patterns;
    }
    return processedPatterns;
  }

  private static ClassDefinition typeCheckClass(Abstract.ClassDefinition def, ClassDefinition enclosingClass, CheckTypeVisitor visitor) {
    LocalErrorReporter errorReporter = visitor.getErrorReporter();
    List<Binding> context = visitor.getContext();
    List<LevelBinding> lvlContext = visitor.getLevelContext();
    boolean classOk = true;

    FieldSet fieldSet = new FieldSet();
    Set<ClassDefinition> superClasses = new HashSet<>();
    try {
      ClassDefinition typedDef = new ClassDefinition(def, fieldSet, superClasses);
      List<LevelBinding> polyParams = new ArrayList<>();
      for (Abstract.TypeArgument polyArgument : def.getPolyParameters()) {
        if (!isPolyParam(polyArgument)) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Classes can only have level parameters", polyArgument));
          classOk = false;
          continue;
        }
        List<LevelBinding> teleParam = typeCheckPolyParam(polyArgument, def, errorReporter);
        if (teleParam == null) {
          classOk = false;
          continue;
        }
        polyParams.addAll(teleParam);
        lvlContext.addAll(teleParam);
      }
      typedDef.setPolyParams(polyParams);
      typedDef.setThisClass(enclosingClass);
      if (enclosingClass != null) {
        DependentLink thisParam = createThisParam(enclosingClass, new LevelArguments(Level.map(typedDef.getEnclosingPolyParams())));
        context.add(thisParam);
        lvlContext.addAll(typedDef.getPolyParams());
        visitor.setThisClass(enclosingClass, Reference(thisParam));
      }

      for (Abstract.SuperClass aSuperClass : def.getSuperClasses()) {
        CheckTypeVisitor.Result result = visitor.checkType(aSuperClass.getSuperClass(), null);
        if (result == null) {
          classOk = false;
          continue;
        }

        ClassCallExpression typeCheckedSuperClass = result.expression.normalize(NormalizeVisitor.Mode.WHNF).toClassCall();
        if (typeCheckedSuperClass == null) {
          errorReporter.report(new LocalTypeCheckingError("Parent must be a class", aSuperClass.getSuperClass()));
          classOk = false;
          continue;
        }

        fieldSet.addFieldsFrom(typeCheckedSuperClass.getFieldSet());
        superClasses.add(typeCheckedSuperClass.getDefinition());

        for (Map.Entry<ClassField, FieldSet.Implementation> entry : typeCheckedSuperClass.getFieldSet().getImplemented()) {
          FieldSet.Implementation oldImpl = fieldSet.getImplementation(entry.getKey());
          if (oldImpl == null || oldImpl.substThisParam(Reference(entry.getValue().thisParam)).equals(entry.getValue().term)) {
            fieldSet.implementField(entry.getKey(), entry.getValue());
          } else {
            classOk = false;
            errorReporter.report(new LocalTypeCheckingError("Implementations of '" + entry.getKey().getName() + "' differ", aSuperClass.getSuperClass()));
          }
        }
      }

      /* if (enclosingClass != null) {
        assert context.size() == 1;
        context.remove(0);
      } else {
        assert context.size() == 0;
      } /**/
      context.clear();

      for (Abstract.ClassField field : def.getFields()) {
        fieldSet.addField(typeCheckClassField(field, typedDef, visitor));
      }

      for (Abstract.Implementation implementation : def.getImplementations()) {
        Definition implementedDef = visitor.getTypecheckingState().getTypechecked(implementation.getImplementedField());
        if (!(implementedDef instanceof ClassField)) {
          classOk = false;
          errorReporter.report(new LocalTypeCheckingError("'" + implementedDef.getName() + "' is not a field", implementation));
          continue;
        }
        ClassField field = (ClassField) implementedDef;
        if (fieldSet.isImplemented(field)) {
          classOk = false;
          errorReporter.report(new LocalTypeCheckingError("Field '" + field.getName() + "' is already implemented", implementation));
          continue;
        }

        DependentLink thisParameter = createThisParam(typedDef, new LevelArguments(Level.map(typedDef.getPolyParams())));
        try (Utils.ContextSaver saver = new Utils.ContextSaver(context)) {
          context.add(thisParameter);
          visitor.setThisClass(typedDef, Reference(thisParameter));
          CheckTypeVisitor.Result result = implementField(fieldSet, field, implementation.getImplementation(), visitor, thisParameter);
          if (result == null || result.expression.toError() != null) {
            classOk = false;
          }
        }
      }

      visitor.getTypecheckingState().record(def, typedDef);
      typedDef.hasErrors(classOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.HAS_ERRORS);
      return typedDef;
    } catch (Namespace.InvalidNamespaceException e) {
      errorReporter.report(e.toError());
    }
    return null;
  }

  private static CheckTypeVisitor.Result implementField(FieldSet fieldSet, ClassField field, Abstract.Expression implBody, CheckTypeVisitor visitor, DependentLink thisParam) {
    CheckTypeVisitor.Result result = visitor.checkType(implBody, field.getBaseType().subst(field.getThisParameter(), Reference(thisParam)));
    fieldSet.implementField(field, new FieldSet.Implementation(thisParam, result != null ? result.expression : Error(null, null)));
    return result;
  }

  private static ClassField typeCheckClassField(Abstract.ClassField def, ClassDefinition enclosingClass, CheckTypeVisitor visitor) {
    DependentLink thisParameter = createThisParam(enclosingClass, new LevelArguments(Level.map(enclosingClass.getPolyParams())));
    visitor.setThisClass(enclosingClass, Reference(thisParameter));
    CheckTypeVisitor.Result typeResult;
    try (Utils.ContextSaver saver = new Utils.ContextSaver(visitor.getContext())) {
      visitor.getContext().add(thisParameter);
      visitor.getLevelContext().addAll(enclosingClass.getPolyParams());
      typeResult = visitor.checkType(def.getResultType(), TypeOmega.getInstance());
    }

    ClassField typedDef = new ClassField(def, typeResult == null ? Error(null, null) : typeResult.expression, enclosingClass, thisParameter);
    visitor.getTypecheckingState().record(def, typedDef);
    return typedDef;
  }

  private static FunctionDefinition typeCheckClassViewInstance(Abstract.ClassViewInstance def, CheckTypeVisitor visitor) {
    LocalErrorReporter errorReporter = visitor.getErrorReporter();
    TypecheckerState state = visitor.getTypecheckingState();

    LinkList list = new LinkList();
    List<LevelBinding> polyParamsList = new ArrayList<>();
    boolean paramsOk = typeCheckParameters(def.getArguments(), def, visitor.getContext(), visitor.getLevelContext(), polyParamsList, list, visitor, null, null);
    FunctionDefinition typedDef = new FunctionDefinition(def, list.getFirst(), null, null);
    typedDef.setPolyParams(polyParamsList);
    state.record(def, typedDef);
    if (!paramsOk) {
      return typedDef;
    }

    Abstract.ClassView classView = (Abstract.ClassView) def.getClassView().getReferent();
    Map<ClassField, Abstract.ClassFieldImpl> classFieldMap = new HashMap<>();
    for (Abstract.ClassFieldImpl classFieldImpl : def.getClassFieldImpls()) {
      ClassField field = (ClassField) visitor.getTypecheckingState().getTypechecked(classFieldImpl.getImplementedField());
      if (classFieldMap.containsKey(field)) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Field '" + field.getName() + "' is already implemented", classFieldImpl));
      } else {
        classFieldMap.put(field, classFieldImpl);
      }
    }

    FieldSet fieldSet = new FieldSet();
    ClassDefinition classDef = (ClassDefinition) visitor.getTypecheckingState().getTypechecked(classView.getUnderlyingClassDefCall().getReferent());
    fieldSet.addFieldsFrom(classDef.getFieldSet());
    ClassCallExpression term = ExpressionFactory.ClassCall(classDef, new LevelArguments(), fieldSet);
    for (ClassField field : classDef.getFieldSet().getFields()) {
      Abstract.ClassFieldImpl impl = classFieldMap.get(field);
      if (impl != null) {
        visitor.implementField(fieldSet, field, impl.getImplementation(), term);
        classFieldMap.remove(field);
      } else {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Field '" + field.getName() + "' is not implemented", def));
        return typedDef;
      }
    }

    FieldSet.Implementation impl = fieldSet.getImplementation((ClassField) state.getTypechecked(classView.getClassifyingField()));
    DefCallExpression defCall = impl.term.normalize(NormalizeVisitor.Mode.WHNF).toDefCall();
    if (defCall == null || !defCall.getDefCallArguments().isEmpty()) {
      errorReporter.report(new LocalTypeCheckingError("Expected a definition in the classifying field", def));
      return typedDef;
    }

    typedDef.setResultType(term);
    typedDef.setElimTree(top(list.getFirst(), leaf(Abstract.Definition.Arrow.RIGHT, New(term))));
    typedDef.typeHasErrors(false);
    typedDef.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);
    return typedDef;
  }
}
