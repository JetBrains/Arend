package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.FindMatchOnIntervalVisitor;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.LevelMax;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelArguments;
import com.jetbrains.jetpad.vclang.term.expr.type.PiTypeOmega;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.PatternsToElimTreeConversion;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.CompositeInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.EmptyInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.LocalInstancePool;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.size;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.toPatterns;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<ClassDefinition, Definition> {
  private final TypecheckerState myState;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final LocalErrorReporter myErrorReporter;

  public DefinitionCheckTypeVisitor(TypecheckerState state, StaticNamespaceProvider staticNamespaceProvider, DynamicNamespaceProvider dynamicNsProvider, LocalErrorReporter errorReporter) {
    myState = state;
    myStaticNsProvider = staticNamespaceProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myErrorReporter = errorReporter;
  }

  public static void typeCheck(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ClassDefinition enclosingClass, Abstract.Definition definition, LocalErrorReporter errorReporter, boolean isPrelude) {
    if (state.getTypechecked(definition) == null) {
      definition.accept(new DefinitionCheckTypeVisitor(state, staticNsProvider, dynamicNsProvider, errorReporter), enclosingClass);
      if (isPrelude) {
        Prelude.update(definition, state.getTypechecked(definition));
      }
    }
  }

  public static void typeCheck(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ClassDefinition enclosingClass, Abstract.Definition definition, LocalErrorReporter errorReporter) {
    typeCheck(state, staticNsProvider, dynamicNsProvider, enclosingClass, definition, errorReporter, false);
  }

  private DependentLink createThisParam(ClassDefinition enclosingClass) {
    assert enclosingClass != null;
    return param("\\this", ClassCall(enclosingClass));
  }

  private static boolean isPolyParam(Abstract.TypeArgument arg) {
    if (arg.getType() instanceof Abstract.DefCallExpression) {
      String typeName = ((Abstract.DefCallExpression) arg.getType()).getName();
      return typeName.equals(Prelude.LVL.getName()) || typeName.equals(Prelude.CNAT.getName());
    }
    return false;
  }

  private static DefCallExpression levelTypeByName(String typeName) {
    if (typeName.equals("Lvl")) {
      return Lvl();
    } else if (typeName.equals("CNat")) {
      return CNat();
    }
    return null;
  }

  private TypedBinding visitPolyParam(Abstract.TypeArgument typeArgument, Map<String, TypedBinding> polyParams, Abstract.SourceNode node) {
    assert (typeArgument.getType() instanceof Abstract.DefCallExpression);
    String typeName = ((Abstract.DefCallExpression) typeArgument.getType()).getName();
    if (!(typeArgument instanceof Abstract.TelescopeArgument)) {
      myErrorReporter.report(new LocalTypeCheckingError("Parameter of type " + typeName + " must have name", node));
      return null;
    }
    Abstract.TelescopeArgument teleArgument = (Abstract.TelescopeArgument)typeArgument;
    if (teleArgument.getNames().size() > 1 || polyParams.containsKey(typeName)) {
      myErrorReporter.report(new LocalTypeCheckingError("Function definition must have at most one polymorphic variable of type " + typeName, node));
      return null;
    }
    if (teleArgument.getExplicit()) {
      myErrorReporter.report(new LocalTypeCheckingError("Polymorphic variables must be implicit", node));
      return null;
    }
    TypedBinding levelParam = new TypedBinding(((Abstract.TelescopeArgument) typeArgument).getNames().get(0), levelTypeByName(typeName));
    polyParams.put(typeName, levelParam);
    return levelParam;
  }

  private Expression typeOmegaToUniverse(PiTypeOmega type, List<TypedBinding> polyParams) {
    TypedBinding lpParam;
    TypedBinding lhParam;

    if (polyParams.isEmpty()) {
      lpParam = new TypedBinding("lp-gen", Lvl());
      lhParam = new TypedBinding("lh-gen", CNat());
      polyParams.add(lpParam);
      polyParams.add(lhParam);
    } else {
      lpParam = polyParams.get(0);
      lhParam = polyParams.get(1);
    }

    Expression cod = Universe(new Level(lpParam), new Level(lhParam));
    return type.getPiParameters().hasNext() ? Pi(type.getPiParameters(), cod) : cod;
  }

  private boolean visitParameters(List<? extends Abstract.Argument> arguments, Abstract.SourceNode node, List<Binding> context, List<TypedBinding> polyParamsList, List<TypedBinding> generatedPolyParams, LinkList list, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    boolean ok = true;
    Map<String, TypedBinding> polyParamsMap = new HashMap<>();
    int index = 0;

    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument)argument;

        if (isPolyParam(typeArgument)) {
          TypedBinding levelParam = visitPolyParam(typeArgument, polyParamsMap, node);
          if (levelParam == null) {
            ok = false;
            continue;
          }
          context.add(levelParam);
          polyParamsList.add(levelParam);
          ++index;
          continue;
        }

        Type paramType = visitor.checkParamType(typeArgument.getType());
        if (paramType == null) {
          ok = false;
          continue;
        }

        if (paramType instanceof PiTypeOmega) {
          boolean firstTime = generatedPolyParams.isEmpty();
          paramType = typeOmegaToUniverse((PiTypeOmega) paramType, generatedPolyParams);
          if (firstTime) {
            context.addAll(generatedPolyParams);
          }
        }

        // paramType = paramType.strip(new HashSet<>(visitor.getContext()), visitor.getErrorReporter());

        Abstract.ClassView classView = Abstract.getUnderlyingClassView(typeArgument.getType());
        if (classView != null) {
          paramType = new ClassViewCallExpression(paramType.toExpression().toClassCall().getDefinition(), paramType.toExpression().toClassCall().getPolyArguments(), paramType.toExpression().toClassCall().getFieldSet(), classView);
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

        paramType = paramType.normalize(NormalizeVisitor.Mode.WHNF);
        for (DependentLink link = param; link.hasNext(); link = link.getNext()) {
          if (localInstancePool != null && paramType.toExpression() != null) {
            Expression type = paramType.toExpression();
            if (type instanceof ClassViewCallExpression) {
              Abstract.ClassView classView1 = ((ClassViewCallExpression) type).getClassView();
              if (classView1.getClassifyingField() != null) {
                ReferenceExpression reference = new ReferenceExpression(link);
                if (!localInstancePool.addInstance(FieldCall((ClassField) myState.getTypechecked(((ClassViewCallExpression) type).getClassView().getClassifyingField()), reference).normalize(NormalizeVisitor.Mode.NF), classView1, reference)) {
                  myErrorReporter.report(new LocalTypeCheckingError(Error.Level.WARNING, "Duplicate instance", argument)); // FIXME[error] better error message
                }
              }
            }
          }
        }

        list.append(param);
        context.addAll(toContext(param));
      } else {
        myErrorReporter.report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0]));
        ok = false;
      }
    }
    return ok;
  }

  @Override
  public FunctionDefinition visitFunction(final Abstract.FunctionDefinition def, ClassDefinition enclosingClass) {
    final FunctionDefinition typedDef = new FunctionDefinition(def);
    myState.record(def, typedDef);
    // TODO[scopes] Fill namespace

    final List<Binding> context = new ArrayList<>();
    LinkList list = new LinkList();
    LocalInstancePool localInstancePool = new LocalInstancePool();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).instancePool(new CompositeInstancePool(localInstancePool, myState.getInstancePool())).build();
    if (enclosingClass != null) {
      DependentLink thisParam = createThisParam(enclosingClass);
      context.add(thisParam);
      list.append(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
      typedDef.setThisClass(enclosingClass);
    }

    List<TypedBinding> polyParamsList = new ArrayList<>();
    List<TypedBinding> generatedPolyParams = new ArrayList<>();
    boolean paramsOk = visitParameters(def.getArguments(), def, context, polyParamsList, generatedPolyParams, list, visitor, localInstancePool);

    TypeMax expectedType = null;
    boolean generatedExpectedType = false;
    Type expectedTypeErased = null;
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      expectedType = visitor.checkFunOrDataType(resultType); //visitor.checkType(resultType, Universe());
      if (expectedType != null) {
        if (expectedType.toExpression() != null) {
          expectedTypeErased = expectedType.toExpression();
        } else if (expectedType instanceof PiTypeOmega) {
          expectedType = typeOmegaToUniverse((PiTypeOmega) expectedType, generatedPolyParams);
          generatedExpectedType = true;
          expectedTypeErased = (Expression) expectedType;
        } else {
          expectedTypeErased = PiTypeOmega.toPiTypeOmega(expectedType);
        }
      }
    }

    typedDef.setParameters(list.getFirst());
    typedDef.setResultType(expectedType);
    polyParamsList.addAll(generatedPolyParams);
    typedDef.setPolyParams(polyParamsList);
    typedDef.typeHasErrors(!paramsOk || expectedType == null);
    typedDef.hasErrors(Definition.TypeCheckingStatus.TYPE_CHECKING);

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      if (term instanceof Abstract.ElimExpression) {
        context.subList(context.size() - size(list.getFirst()), context.size()).clear();
        ElimTreeNode elimTree = visitor.getTypeCheckingElim().typeCheckElim((Abstract.ElimExpression) term, def.getArrow() == Abstract.Definition.Arrow.LEFT ? list.getFirst() : null, expectedTypeErased, false, true);
        if (elimTree != null) {
          typedDef.setElimTree(elimTree);
          typedDef.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);
        }
      } else {
        CheckTypeVisitor.Result termResult = visitor.checkType(term, expectedTypeErased);
        if (termResult != null) {
          if (!generatedExpectedType && expectedType != null && termResult.getType().toSorts() != null && !termResult.getType().toSorts().isLessOrEquals(expectedType.toSorts())) {
            myErrorReporter.report(new TypeMismatchError(expectedType, termResult.getType(), term));
          } else {
            typedDef.setElimTree(top(list.getFirst(), leaf(def.getArrow(), termResult.getExpression())));
            if (generatedExpectedType && expectedType != null) {
              typedDef.setResultType(expectedType.toSorts().max(termResult.getType().toSorts()).toType());
            } else if (expectedType == null) {
              typedDef.setResultType(termResult.getType());
            }
            typedDef.typeHasErrors(!paramsOk || typedDef.getResultType() == null);
          }
        }
      }

      if (typedDef.getElimTree() != null) {
        if (!typedDef.getElimTree().accept(new TerminationCheckVisitor(typedDef, typedDef.getParameters()), null)) {
          myErrorReporter.report(new LocalTypeCheckingError("Termination check failed", term));
          typedDef.setElimTree(null);
        }
      }

      if (typedDef.getElimTree() != null) {
        LocalTypeCheckingError error = TypeCheckingElim.checkCoverage(def, list.getFirst(), typedDef.getElimTree(), expectedTypeErased);
        if (error != null) {
          myErrorReporter.report(error);
        }
      }

      if (typedDef.getElimTree() != null) {
        LocalTypeCheckingError error = TypeCheckingElim.checkConditions(def, list.getFirst(), typedDef.getElimTree());
        if (error != null) {
          myErrorReporter.report(error);
          typedDef.setElimTree(null);
        }
      }
    }

    typedDef.hasErrors(typedDef.getElimTree() != null ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.HAS_ERRORS);
    return typedDef;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, ClassDefinition enclosingClass) {
    List<Binding> context = new ArrayList<>();
    LinkList list = new LinkList();
    LocalInstancePool localInstancePool = new LocalInstancePool();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).instancePool(new CompositeInstancePool(localInstancePool, myState.getInstancePool())).build();
    List<TypedBinding> polyParamsList = new ArrayList<>();
    List<TypedBinding> generatedPolyParams = new ArrayList<>();
    if (enclosingClass != null) {
      DependentLink thisParam = createThisParam(enclosingClass);
      context.add(thisParam);
      list.append(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
    }

    SortMax inferredSorts = def.getConstructors().size() > 1 ? new SortMax(new Sort(new Level(0), Sort.SET.getHLevel())) : new SortMax();
    SortMax userSorts = null;
    boolean paramsOk;
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
      paramsOk = visitParameters(def.getParameters(), def, context, polyParamsList, generatedPolyParams, list, visitor, localInstancePool);
      // polyParamsList.addAll(generatedPolyParams);

      if (def.getUniverse() != null) {
        if (def.getUniverse() instanceof Abstract.PolyUniverseExpression) {
          userSorts = visitor.sortMax((Abstract.PolyUniverseExpression)def.getUniverse());
        } else if (def.getUniverse() instanceof Abstract.UniverseExpression) {
          CheckTypeVisitor.Result result = visitor.checkType(def.getUniverse(), Universe());
          if (result != null) {
            userSorts = new SortMax(result.getExpression().toUniverse().getSort());
          }
        } else {
          String msg = "Specified type " + PrettyPrintVisitor.prettyPrint(def.getUniverse(), 0) + " of '" + def.getName() + "' is not a universe";
          myErrorReporter.report(new LocalTypeCheckingError(msg, def.getUniverse()));
        }
      }
    }

    DataDefinition dataDefinition = new DataDefinition(def, inferredSorts, list.getFirst());
    dataDefinition.setThisClass(enclosingClass);
    dataDefinition.setPolyParams(polyParamsList);
    if (userSorts != null) {
      dataDefinition.setSorts(userSorts);
    }
    myState.record(def, dataDefinition);

    if (!paramsOk) {
      dataDefinition.typeHasErrors(true);
      dataDefinition.hasErrors(Definition.TypeCheckingStatus.HAS_ERRORS);
      for (Abstract.Constructor constructor : def.getConstructors()) {
        myState.record(constructor, new Constructor(constructor, dataDefinition));
      }
      return dataDefinition;
    }
    dataDefinition.typeHasErrors(false);
    dataDefinition.hasErrors(Definition.TypeCheckingStatus.TYPE_CHECKING);

    boolean dataOk = true;
    for (Abstract.Constructor constructor : def.getConstructors()) {
      visitor.getContext().clear();
      Constructor typedConstructor = visitConstructor(constructor, dataDefinition, visitor, inferredSorts, generatedPolyParams);
      myState.record(constructor, typedConstructor);
      if (typedConstructor.typeHasErrors()) {
        dataOk = false;
      }
    }
    dataDefinition.hasErrors(dataOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.HAS_ERRORS);
    if (!generatedPolyParams.isEmpty()) {
      polyParamsList.addAll(generatedPolyParams);
      dataDefinition.setPolyParams(polyParamsList);
    }

    context.clear();
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
        myErrorReporter.report(error);
      }
    }

    if (!dataDefinition.getConditions().isEmpty()) {
      List<Condition> failedConditions = new ArrayList<>();
      for (Condition condition : dataDefinition.getConditions()) {
        LocalTypeCheckingError error = TypeCheckingElim.checkConditions(condition.getConstructor().getName(), def, condition.getConstructor().getParameters(), condition.getElimTree());
        if (error != null) {
          myErrorReporter.report(error);
          failedConditions.add(condition);
          dataDefinition.hasErrors(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
      }
      dataDefinition.getConditions().removeAll(failedConditions);
      for (Condition condition : dataDefinition.getConditions()) {
        if (condition.getElimTree().accept(new FindMatchOnIntervalVisitor(), null)) {
          dataDefinition.setMatchesOnInterval();
          inferredSorts = new SortMax(inferredSorts.getPLevel(), LevelMax.INFINITY);
        }
      }
    }

    if (userSorts != null) {
      if (inferredSorts.isLessOrEquals(userSorts)) {
        dataDefinition.setSorts(userSorts);
      } else {
        String msg = "Actual universe " + inferredSorts + " is not compatible with expected universe " + userSorts;
        myErrorReporter.report(new LocalTypeCheckingError(msg, def.getUniverse()));
        dataDefinition.setSorts(inferredSorts);
      }
    } else {
      dataDefinition.setSorts(inferredSorts);
    }

    return dataDefinition;
  }

  private List<Constructor> typeCheckConditions(CheckTypeVisitor visitor, DataDefinition dataDefinition, Abstract.DataDefinition def) {
    Map<Constructor, List<Abstract.Condition>> condMap = new HashMap<>();
    for (Abstract.Condition cond : def.getConditions()) {
      Constructor constructor = dataDefinition.getConstructor(cond.getConstructorName());
      if (constructor == null) {
        myErrorReporter.report(new NotInScopeError(def, cond.getConstructorName()));  // TODO: refer by reference
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
    List<Constructor> cycle = searchConditionCycle(condMap);
    if (cycle != null) {
      return cycle;
    }
    for (Constructor constructor : condMap.keySet()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
        final List<List<Pattern>> patterns = new ArrayList<>();
        final List<Expression> expressions = new ArrayList<>();
        final List<Abstract.Definition.Arrow> arrows = new ArrayList<>();
        visitor.getContext().addAll(toContext(constructor.getDataTypeParameters()));

        for (Abstract.Condition cond : condMap.get(constructor)) {
          try (Utils.ContextSaver saver = new Utils.ContextSaver(visitor.getContext())) {
            List<Expression> resultType = new ArrayList<>(Collections.singletonList(constructor.getDataTypeExpression(new LevelArguments())));
            DependentLink params = constructor.getParameters();
            List<Abstract.PatternArgument> processedPatterns = processImplicitPatterns(cond, params, cond.getPatterns());
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
            expressions.add(result.getExpression().normalize(NormalizeVisitor.Mode.NF));
            arrows.add(Abstract.Definition.Arrow.RIGHT);
          }
        }

        PatternsToElimTreeConversion.OKResult elimTreeResult = (PatternsToElimTreeConversion.OKResult) PatternsToElimTreeConversion.convert(constructor.getParameters(), patterns, expressions, arrows);

        if (!elimTreeResult.elimTree.accept(new TerminationCheckVisitor(constructor, constructor.getDataTypeParameters(), constructor.getParameters()), null)) {
          myErrorReporter.report(new LocalTypeCheckingError("Termination check failed", null));
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

  private List<Constructor> searchConditionCycle(Map<Constructor, List<Abstract.Condition>> condMap) {
    Set<Constructor> visited = new HashSet<>();
    List<Constructor> visiting = new ArrayList<>();
    for (Constructor constructor : condMap.keySet()) {
      List<Constructor> cycle = searchConditionCycle(condMap, constructor, visited, visiting);
      if (cycle != null)
        return cycle;
    }
    return null;
  }

  private List<Constructor> searchConditionCycle(Map<Constructor, List<Abstract.Condition>> condMap, Constructor constructor, Set<Constructor> visited, List<Constructor> visiting) {
    if (visited.contains(constructor))
      return null;
    if (visiting.contains(constructor)) {
      return visiting.subList(visiting.lastIndexOf(constructor), visiting.size());
    }
    visiting.add(constructor);
    if (condMap.containsKey(constructor)) {
      for (Abstract.Condition condition : condMap.get(constructor)) {
        Set<Abstract.Definition> dependencies = new HashSet<>();
        condition.getTerm().accept(new CollectDefCallsVisitor(dependencies), null);
        for (Abstract.Definition def : dependencies) {
          final Definition typeCheckedDef = myState.getTypechecked(def);
          if (typeCheckedDef != null && typeCheckedDef != constructor && typeCheckedDef instanceof Constructor && ((Constructor) typeCheckedDef).getDataType().equals(constructor.getDataType())) {
            List<Constructor> cycle = searchConditionCycle(condMap, (Constructor) typeCheckedDef, visited, visiting);
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

  @Override
  public Definition visitConstructor(Abstract.Constructor def, ClassDefinition enclosingClass) {
    throw new IllegalStateException();
  }

  private Constructor visitConstructor(Abstract.Constructor def, DataDefinition dataDefinition, CheckTypeVisitor visitor, SortMax sorts, List<TypedBinding> generatedPolyParams) {
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
        processedPatterns = processImplicitPatterns(def, dataDefinition.getParameters(), processedPatterns);
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
        Type paramType = visitor.checkParamType(argument.getType());
        if (paramType == null) {
          return constructor;
        }
//        paramType = paramType.strip(new HashSet<>(visitor.getContext()), visitor.getErrorReporter());

        if (paramType instanceof PiTypeOmega) {
          boolean firstTime = generatedPolyParams.isEmpty();
          paramType = typeOmegaToUniverse((PiTypeOmega) paramType, generatedPolyParams);
          if (firstTime) {
            visitor.getContext().addAll(generatedPolyParams);
          }
        }

        sorts.add(paramType.toExpression() != null ? paramType.toExpression().getType().toSorts() : SortMax.OMEGA);

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          param = param(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), paramType);
        } else {
          param = param(argument.getExplicit(), (String) null, paramType);
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
          if (!checkNonPositiveError(piParam.getType().toExpression(), dataDefinition, name, list.getFirst(), link, arguments, def)) {
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
                if (!checkNonPositiveError(expr, dataDefinition, name, list.getFirst(), link, arguments, def)) {
                  return constructor;
                }
              }
            } else {
              if (!checkNonPositiveError(type.toExpression(), dataDefinition, name, list.getFirst(), link, arguments, def)) {
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

      myState.record(def, constructor);
      return constructor;
    }
  }

  private boolean checkNonPositiveError(Expression expr, DataDefinition dataDefinition, String name, DependentLink params, DependentLink param, List<? extends Abstract.Argument> args, Abstract.Constructor constructor) {
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
    myErrorReporter.report(new LocalTypeCheckingError(msg, argument == null ? constructor : argument));
    return false;
  }

  private List<Abstract.PatternArgument> processImplicitPatterns(Abstract.SourceNode expression, DependentLink parameters, List<? extends Abstract.PatternArgument> patterns) {
    List<Abstract.PatternArgument> processedPatterns = null;
    ProcessImplicitResult processImplicitResult = processImplicit(patterns, parameters);
    if (processImplicitResult.patterns == null) {
      if (processImplicitResult.numExcessive != 0) {
        myErrorReporter.report(new LocalTypeCheckingError("Too many arguments: " + processImplicitResult.numExcessive + " excessive", expression));
      } else if (processImplicitResult.wrongImplicitPosition < patterns.size()) {
        myErrorReporter.report(new LocalTypeCheckingError("Unexpected implicit argument", patterns.get(processImplicitResult.wrongImplicitPosition)));
      } else {
        myErrorReporter.report(new LocalTypeCheckingError("Too few explicit arguments, expected: " + processImplicitResult.numExplicit, expression));
      }
    } else {
      processedPatterns = processImplicitResult.patterns;
    }
    return processedPatterns;
  }

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, ClassDefinition enclosingClass) {
    boolean classOk = true;
    List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).instancePool(EmptyInstancePool.INSTANCE).build();

    if (enclosingClass != null) {
      DependentLink thisParam = createThisParam(enclosingClass);
      context.add(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
    }

    FieldSet fieldSet = new FieldSet();
    Set<ClassDefinition> superClasses = new HashSet<>();
    try {
      ClassDefinition typedDef = new ClassDefinition(def, fieldSet, superClasses);
      typedDef.setThisClass(enclosingClass);

      for (Abstract.SuperClass aSuperClass : def.getSuperClasses()) {
        CheckTypeVisitor.Result result = aSuperClass.getSuperClass().accept(visitor, Universe());
        if (result == null) {
          classOk = false;
          continue;
        }

        ClassCallExpression typeCheckedSuperClass = result.getExpression().toClassCall();
        if (typeCheckedSuperClass == null) {
          myErrorReporter.report(new LocalTypeCheckingError("Parent must be a class", aSuperClass.getSuperClass()));
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
            myErrorReporter.report(new LocalTypeCheckingError("Implementations of '" + entry.getKey().getName() + "' differ", aSuperClass.getSuperClass()));
          }
        }
      }

      if (enclosingClass != null) {
        assert context.size() == 1;
        context.remove(0);
      } else {
        assert context.size() == 0;
      }

      for (Abstract.ClassField field : def.getFields()) {
        fieldSet.addField(visitClassField(field, typedDef));
      }

      for (Abstract.Implementation implementation : def.getImplementations()) {
        Definition implementedDef = myState.getTypechecked(implementation.getImplementedField());
        if (!(implementedDef instanceof ClassField)) {
          classOk = false;
          myErrorReporter.report(new LocalTypeCheckingError("'" + implementedDef.getName() + "' is not a field", implementation));
          continue;
        }
        ClassField field = (ClassField) implementedDef;
        if (fieldSet.isImplemented(field)) {
          classOk = false;
          myErrorReporter.report(new LocalTypeCheckingError("Field '" + field.getName() + "' is already implemented", implementation));
          continue;
        }

        DependentLink thisParameter = createThisParam(typedDef);
        try (Utils.ContextSaver saver = new Utils.ContextSaver(context)) {
          context.add(thisParameter);
          visitor.setThisClass(typedDef, Reference(thisParameter));
          CheckTypeVisitor.Result result = implementField(fieldSet, field, implementation.getImplementation(), visitor, thisParameter);
          if (result == null || result.getExpression().toError() != null) {
            classOk = false;
          }
        }
      }

      myState.record(def, typedDef);
      typedDef.hasErrors(!classOk);
      return typedDef;
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }
    return null;
  }

  private CheckTypeVisitor.Result implementField(FieldSet fieldSet, ClassField field, Abstract.Expression implBody, CheckTypeVisitor visitor, DependentLink thisParam) {
    CheckTypeVisitor.Result result = visitor.checkType(implBody, field.getBaseType().subst(field.getThisParameter(), Reference(thisParam)));
    fieldSet.implementField(field, new FieldSet.Implementation(thisParam, result != null ? result.getExpression() : Error(null, null)));
    return result;
  }

  @Override
  public ClassField visitClassField(Abstract.ClassField def, ClassDefinition enclosingClass) {
    if (enclosingClass == null) throw new IllegalStateException();

    List<? extends Abstract.Argument> arguments = def.getArguments();
    Expression typedResultType;
    DependentLink thisParameter = createThisParam(enclosingClass);
    List<Binding> context = new ArrayList<>();
    context.add(thisParameter);
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).instancePool(EmptyInstancePool.INSTANCE).thisClass(enclosingClass, Reference(thisParameter)).build();
    ClassField typedDef = new ClassField(def, Error(null, null), enclosingClass, thisParameter);
    myState.record(def, typedDef);

    Map<String, TypedBinding> polyParams = new HashMap<>();
    int index = 0;
    LinkList list = new LinkList();
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) argument;

        if (isPolyParam(typeArgument)) {
          Binding levelParam = visitPolyParam(typeArgument, polyParams, def);
          if (levelParam == null) {
            return typedDef;
          }
          context.add(levelParam);
          ++index;
          continue;
        }

        CheckTypeVisitor.Result result = visitor.checkType(((Abstract.TypeArgument) argument).getType(), new PiTypeOmega(EmptyDependentLink.getInstance()));
        if (result == null) {
          return typedDef;
        }

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
          param = param(argument.getExplicit(), names, result.getExpression());
          index += names.size();
        } else {
          param = param(argument.getExplicit(), (String) null, result.getExpression());
          index++;
        }
        list.append(param);
        context.addAll(toContext(param));
      } else {
        myErrorReporter.report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0]));
        return typedDef;
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType == null) {
      return typedDef;
    }
    CheckTypeVisitor.Result typeResult = visitor.checkType(resultType, new PiTypeOmega(EmptyDependentLink.getInstance()));
    if (typeResult == null) {
      return typedDef;
    }
    typedResultType = typeResult.getExpression();

    typedDef.setPolyParams(new ArrayList<>(polyParams.values()));
    typedDef.setBaseType(list.isEmpty() ? typedResultType : Pi(list.getFirst(), typedResultType));
    typedDef.setThisClass(enclosingClass);
    return typedDef;
  }

  @Override
  public Definition visitImplement(Abstract.Implementation def, ClassDefinition params) {
    throw new IllegalStateException();
  }

  @Override
  public Definition visitClassView(Abstract.ClassView def, ClassDefinition params) {
    throw new IllegalStateException();
  }

  @Override
  public Definition visitClassViewField(Abstract.ClassViewField def, ClassDefinition params) {
    throw new IllegalStateException();
  }

  @Override
  public Definition visitClassViewInstance(Abstract.ClassViewInstance def, ClassDefinition enclosingClass) {
    FunctionDefinition typedDef = new FunctionDefinition(def);
    myState.record(def, typedDef);

    final List<Binding> context = new ArrayList<>();
    LinkList list = new LinkList();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).build();

    List<TypedBinding> polyParamsList = new ArrayList<>();
    List<TypedBinding> generatedPolyParams = new ArrayList<>();
    boolean paramsOk = visitParameters(def.getArguments(), def, context, polyParamsList, generatedPolyParams, list, visitor, null);
    polyParamsList.addAll(generatedPolyParams);
    typedDef.setPolyParams(polyParamsList);
    typedDef.setParameters(list.getFirst());

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      if (term instanceof Abstract.ElimExpression) {
        myErrorReporter.report(new LocalTypeCheckingError("\\elim is not allowed in \\instance", def));
      } else {
        CheckTypeVisitor.Result termResult = visitor.checkType(term, null);
        if (termResult != null) {
          typedDef.setResultType(termResult.getType());
          typedDef.typeHasErrors(!paramsOk || termResult.getType() == null);
          typedDef.setElimTree(top(list.getFirst(), leaf(Abstract.Definition.Arrow.RIGHT, termResult.getExpression())));
          typedDef.hasErrors(paramsOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.HAS_ERRORS);
        }
      }

      if (typedDef.getElimTree() != null) {
        if (!typedDef.getElimTree().accept(new TerminationCheckVisitor(typedDef, typedDef.getParameters()), null)) {
          // FIXME[errorformat]
          myErrorReporter.report(new LocalTypeCheckingError("Termination check failed", term));
          typedDef.setElimTree(null);
          typedDef.hasErrors(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
      } else {
        typedDef.hasErrors(Definition.TypeCheckingStatus.HAS_ERRORS);
      }
    }

    if (typedDef.hasErrors() == Definition.TypeCheckingStatus.NO_ERRORS) {
      Expression expr = typedDef.getResultType().toExpression();
      if (expr != null) {
        expr = expr.normalize(NormalizeVisitor.Mode.WHNF);
      }
      if (expr == null || expr.toClassCall() == null || !(expr.toClassCall() instanceof ClassViewCallExpression)) {
        myErrorReporter.report(new LocalTypeCheckingError("Expected an expression of a class view type", term));
        return typedDef;
      }

      Abstract.ClassView classView = ((ClassViewCallExpression) expr.toClassCall()).getClassView();
      FieldSet.Implementation impl = expr.toClassCall().getFieldSet().getImplementation((ClassField) myState.getTypechecked(classView.getClassifyingField()));
      if (impl == null) {
        myErrorReporter.report(new LocalTypeCheckingError("Classifying field is not implemented", term));
        return typedDef;
      }
      DefCallExpression defCall = impl.term.normalize(NormalizeVisitor.Mode.WHNF).toDefCall();
      if (defCall == null || !defCall.getDefCallArguments().isEmpty()) {
        myErrorReporter.report(new LocalTypeCheckingError("Expected a definition in the classifying field", term));
        return typedDef;
      }
      if (myState.getInstancePool().getInstance(defCall.getDefinition(), classView) != null) {
        myErrorReporter.report(new LocalTypeCheckingError("Instance of '" + classView.getName() + "' for '" + defCall.getDefinition().getName() + "' is already defined", term));
      } else {
        Expression instance = FunCall(typedDef, new LevelArguments(), Collections.<Expression>emptyList());
        myState.getInstancePool().addInstance(defCall.getDefinition(), classView, instance);
        if (def.isDefault()) {
          if (myState.getInstancePool().getInstance(defCall.getDefinition(), null) != null) {
            myErrorReporter.report(new LocalTypeCheckingError("Default instance of '" + classView.getName() + "' for '" + defCall.getDefinition().getName() + "' is already defined", term));
          } else {
            myState.getInstancePool().addInstance(defCall.getDefinition(), null, instance);
          }
        }
      }
    }

    return typedDef;
  }
}
