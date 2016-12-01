package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.parser.prettyprint.PrettyPrintVisitor;
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
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CollectDefCallsVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.TerminationCheckVisitor;
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
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;
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

  public static CheckTypeVisitor typeCheckHeader(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, Abstract.Definition definition, Abstract.ClassDefinition enclosingClass, LocalErrorReporter errorReporter) {
    if (state.getTypechecked(definition) == null) {
      LocalInstancePool localInstancePool = new LocalInstancePool();
      CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(state, staticNsProvider, dynamicNsProvider, new ArrayList<Binding>(), errorReporter).instancePool(new CompositeInstancePool(localInstancePool, state.getInstancePool())).build();
      ClassDefinition typedEnclosingClass = enclosingClass == null ? null : (ClassDefinition) state.getTypechecked(enclosingClass);

      // if (definition instanceof Abstract.FunctionDefinition) {
      //
      // } else
      if (definition instanceof Abstract.DataDefinition) {
        Definition typechecked = typeCheckDataHeader((Abstract.DataDefinition) definition, typedEnclosingClass, visitor, localInstancePool);
        return typechecked.hasErrors() == Definition.TypeCheckingStatus.TYPE_CHECKING ? visitor : null;
      } else {
        throw new IllegalStateException();
      }
    }
    return null;
  }

  public static void typeCheckBody(Definition definition, CheckTypeVisitor exprVisitor) {
    // if (definition instanceof FunctionDefinition) {
    //   visitor.typeCheckFunctionBody((FunctionDefinition) definition);
    // } else
    if (definition instanceof DataDefinition) {
      typeCheckDataBody((DataDefinition) definition, exprVisitor);
    } else {
      throw new IllegalStateException();
    }
  }

  public static void typeCheck(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, TypecheckingUnit unit, LocalErrorReporter errorReporter) {
    assert !(unit.getDefinition() instanceof Abstract.DataDefinition);
    // assert !(unit.getDefinition() instanceof Abstract.FunctionDefinition);
    Definition typechecked = state.getTypechecked(unit.getDefinition());
    if (typechecked != null) {
      assert typechecked.hasErrors() != Definition.TypeCheckingStatus.TYPE_CHECKING;
      return;
    }

    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(state, staticNsProvider, dynamicNsProvider, errorReporter);
    ClassDefinition enclosingClass = unit.getEnclosingClass() == null ? null : (ClassDefinition) state.getTypechecked(unit.getEnclosingClass());
    typechecked = unit.getDefinition().accept(visitor, enclosingClass);
  }

  private static DependentLink createThisParam(ClassDefinition enclosingClass, LevelArguments polyArgs) {
    assert enclosingClass != null;
    return param("\\this", ClassCall(enclosingClass, polyArgs));
  }

  private static boolean isPolyParam(Abstract.TypeArgument arg) {
    if (arg.getType() instanceof Abstract.DefCallExpression) {
      String typeName = ((Abstract.DefCallExpression) arg.getType()).getName();
      // TODO: WTF ???
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

  private static TypedBinding visitPolyParam(Abstract.TypeArgument typeArgument, Abstract.SourceNode node, LocalErrorReporter errorReporter) {
    assert (typeArgument.getType() instanceof Abstract.DefCallExpression);
    String typeName = ((Abstract.DefCallExpression) typeArgument.getType()).getName();
    if (!(typeArgument instanceof Abstract.TelescopeArgument)) {
      errorReporter.report(new LocalTypeCheckingError("Parameter of type " + typeName + " must have name", node));
      return null;
    }
    Abstract.TelescopeArgument teleArgument = (Abstract.TelescopeArgument)typeArgument;
    //if (teleArgument.getNames().size() > 1 || polyParams.containsKey(typeName)) {
    //  myErrorReporter.report(new LocalTypeCheckingError("Function definition must have at most one polymorphic variable of type " + typeName, node));
    //  return null;
   // }
    if (teleArgument.getExplicit()) {
      errorReporter.report(new LocalTypeCheckingError("Polymorphic variables must be implicit", node));
      return null;
    }
    return new TypedBinding(((Abstract.TelescopeArgument) typeArgument).getNames().get(0), levelTypeByName(typeName));
    // polyParams.put(typeName, levelParam);
    // return levelParam;
  }

  private static Expression typeOmegaToUniverse(DependentLink params, List<TypedBinding> polyParams) {
    TypedBinding lpParam;
    TypedBinding lhParam;

    if (polyParams.isEmpty()) {
      lpParam = new TypedBinding("\\lp", Lvl());
      lhParam = new TypedBinding("\\lh", CNat());
      polyParams.add(lpParam);
      polyParams.add(lhParam);
    } else {
      lpParam = polyParams.get(0);
      lhParam = polyParams.get(1);
    }

    Expression cod = Universe(new Level(lpParam), new Level(lhParam));
    return params.hasNext() ? Pi(params, cod) : cod;
  }

  private static boolean visitParameters(List<? extends Abstract.Argument> arguments, Abstract.SourceNode node, List<Binding> context, List<TypedBinding> polyParamsList, LinkList list, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    boolean ok = true;
    boolean polyParamsAllowed = true;
    int index = 0;

    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument)argument;

        if (isPolyParam(typeArgument)) {
          if (!polyParamsAllowed) {
            visitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.ERROR, "Poly parameters must be declared in the beginning of a definition", argument));
            ok = false;
            continue;
          }
          TypedBinding levelParam = visitPolyParam(typeArgument, node, visitor.getErrorReporter());
          if (levelParam == null) {
            ok = false;
            continue;
          }
          context.add(levelParam);
          polyParamsList.add(levelParam);
          ++index;
          continue;
        } else {
          polyParamsAllowed = false;
        }

        Type paramType = visitor.checkParamType(typeArgument.getType());
        if (paramType == null) {
          ok = false;
          continue;
        }

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
        if (localInstancePool != null && paramType.toExpression() != null) {
          Expression type = paramType.toExpression();
          if (type instanceof ClassViewCallExpression) {
            Abstract.ClassView classView1 = ((ClassViewCallExpression) type).getClassView();
            if (classView1.getClassifyingField() != null) {
              for (DependentLink link = param; link.hasNext(); link = link.getNext()) {
                ReferenceExpression reference = new ReferenceExpression(link);
                if (!localInstancePool.addInstance(FieldCall((ClassField) visitor.getTypecheckingState().getTypechecked(((ClassViewCallExpression) type).getClassView().getClassifyingField()), reference).normalize(NormalizeVisitor.Mode.NF), classView1, reference)) {
                  visitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "Duplicate instance", argument)); // FIXME[error] better error message
                }
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
    return ok;
  }

  private void typeCheckFunctionBody(FunctionDefinition def) {

  }

  @Override
  public FunctionDefinition visitFunction(Abstract.FunctionDefinition def, ClassDefinition enclosingClass) {
    FunctionDefinition typedDef = new FunctionDefinition(def);
    myState.record(def, typedDef);

    List<Binding> context = new ArrayList<>();
    List<TypedBinding> polyParamsList = new ArrayList<>();
    LinkList list = new LinkList();
    LocalInstancePool localInstancePool = new LocalInstancePool();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).instancePool(new CompositeInstancePool(localInstancePool, myState.getInstancePool())).build();
    if (enclosingClass != null) {
      for (Binding param : enclosingClass.getPolyParams()) {
        TypedBinding defParam = new TypedBinding(param.getName(), param.getType());
        polyParamsList.add(defParam);
      }
      DependentLink thisParam = createThisParam(enclosingClass, new LevelArguments(Level.map(polyParamsList)));
      context.add(thisParam);
      context.addAll(enclosingClass.getPolyParams());
      list.append(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
      typedDef.setThisClass(enclosingClass);
    }

    boolean paramsOk = visitParameters(def.getArguments(), def, context, polyParamsList, list, visitor, localInstancePool);
    List<TypedBinding> generatedPolyParams = new ArrayList<>();

    TypeMax expectedType = null;
    boolean generatedExpectedType = false;
    Type expectedTypeErased = null;
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      expectedType = visitor.checkFunOrDataType(resultType);
      if (expectedType != null) {
        if (expectedType.toExpression() != null) {
          expectedTypeErased = expectedType.toExpression();
        } else if (expectedType instanceof PiTypeOmega) {
          expectedTypeErased = (Type) expectedType;
          expectedType = typeOmegaToUniverse(expectedType.getPiParameters(), generatedPolyParams);
          generatedExpectedType = true;
        } else {
          expectedTypeErased = PiTypeOmega.toPiTypeOmega(expectedType);
        }
      }
    }

    for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      if (link.getType() instanceof PiTypeOmega) {
        link.setType(typeOmegaToUniverse(link.getType().getPiParameters(), generatedPolyParams));
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
            typedDef.setResultType(termResult.getType());
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

  private static DataDefinition typeCheckDataHeader(Abstract.DataDefinition def, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    LinkList list = new LinkList();
    List<TypedBinding> polyParamsList = new ArrayList<>();
    if (enclosingClass != null) {
      for (Binding param : enclosingClass.getPolyParams()) {
        polyParamsList.add(new TypedBinding(param.getName(), param.getType()));
      }
      DependentLink thisParam = createThisParam(enclosingClass, new LevelArguments(Level.map(polyParamsList)));
      visitor.getContext().add(thisParam);
      visitor.getContext().addAll(polyParamsList);
      list.append(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
    }

    SortMax userSorts = SortMax.OMEGA;
    boolean paramsOk;
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
      paramsOk = visitParameters(def.getParameters(), def, visitor.getContext(), polyParamsList, list, visitor, localInstancePool);

      if (def.getUniverse() != null) {
        if (def.getUniverse() instanceof Abstract.PolyUniverseExpression) {
          userSorts = visitor.sortMax((Abstract.PolyUniverseExpression)def.getUniverse());
        } else
        if (def.getUniverse() instanceof Abstract.UniverseExpression) {
          CheckTypeVisitor.Result result = visitor.checkType(def.getUniverse(), new PiTypeOmega(EmptyDependentLink.getInstance()));
          if (result != null) {
            userSorts = new SortMax(result.getExpression().toUniverse().getSort());
          }
        } else
        if (!(def.getUniverse() instanceof Abstract.TypeOmegaExpression)) {
          String msg = "Specified type " + PrettyPrintVisitor.prettyPrint(def.getUniverse(), 0) + " of '" + def.getName() + "' is not a universe";
          visitor.getErrorReporter().report(new LocalTypeCheckingError(msg, def.getUniverse()));
        }
      }
    }

    DataDefinition dataDefinition = new DataDefinition(def, userSorts, list.getFirst());
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
    SortMax inferredSorts = def.getConstructors().size() > 1 ? new SortMax(new Sort(new Level(0), Sort.SET.getHLevel())) : new SortMax();
    dataDefinition.setSorts(inferredSorts);

    boolean dataOk = true;
    boolean universeOk = true;
    for (Abstract.Constructor constructor : def.getConstructors()) {
      visitor.getContext().clear();
      SortMax conSorts = new SortMax();
      Constructor typedConstructor = visitConstructor(constructor, dataDefinition, visitor, conSorts);
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
      if (inferredSorts.isLessOrEquals(userSorts)) {
        inferredSorts = userSorts;
      } else {
        String msg = "Actual universe " + inferredSorts + " is not compatible with expected universe " + userSorts;
        visitor.getErrorReporter().report(new LocalTypeCheckingError(msg, def.getUniverse()));
      }
    }

    List<TypedBinding> polyParams = new ArrayList<>();
    if (inferredSorts.isOmega()) {
      inferredSorts = new SortMax(typeOmegaToUniverse(EmptyDependentLink.getInstance(), polyParams).toUniverse().getSort());

      for (DependentLink link = dataDefinition.getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (link.getType() instanceof PiTypeOmega) {
          link.setType(typeOmegaToUniverse(link.getType().getPiParameters(), polyParams));
        }
      }

      if (!polyParams.isEmpty()) {
        polyParams.addAll(dataDefinition.getPolyParams());
        dataDefinition.setPolyParams(polyParams);
      }
    }

    dataDefinition.setSorts(inferredSorts);
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, ClassDefinition enclosingClass) {
    throw new IllegalStateException();
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
            List<Expression> resultType = new ArrayList<>(Collections.singletonList(constructor.getDataTypeExpression(new LevelArguments())));
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
            expressions.add(result.getExpression().normalize(NormalizeVisitor.Mode.NF));
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
        condition.getTerm().accept(new CollectDefCallsVisitor(dependencies), null);
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

  @Override
  public Definition visitConstructor(Abstract.Constructor def, ClassDefinition enclosingClass) {
    throw new IllegalStateException();
  }

  private static Constructor visitConstructor(Abstract.Constructor def, DataDefinition dataDefinition, CheckTypeVisitor visitor, SortMax sorts) {
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
        CheckTypeVisitor.Result paramResult = visitor.checkType(argument.getType(), new PiTypeOmega(EmptyDependentLink.getInstance()));
        if (paramResult == null) {
          return constructor;
        }

        sorts.add(paramResult.getType().toSorts());

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          param = param(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), paramResult.getExpression());
        } else {
          param = param(argument.getExplicit(), (String) null, paramResult.getExpression());
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

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, ClassDefinition enclosingClass) {
    boolean classOk = true;
    List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).instancePool(EmptyInstancePool.INSTANCE).build();

    FieldSet fieldSet = new FieldSet();
    Set<ClassDefinition> superClasses = new HashSet<>();
    try {
      ClassDefinition typedDef = new ClassDefinition(def, fieldSet, superClasses);
      List<TypedBinding> polyParams = new ArrayList<>();
      for (Abstract.TypeArgument polyArgument : def.getPolyParameters()) {
        if (!isPolyParam(polyArgument)) {
          myErrorReporter.report(new LocalTypeCheckingError("Classes can only have level parameters", polyArgument));
          classOk = false;
          continue;
        }
        TypedBinding param = visitPolyParam(polyArgument, def, myErrorReporter);
        polyParams.add(param);
        context.add(param);
      }
      typedDef.setPolyParams(polyParams);
      typedDef.setThisClass(enclosingClass);
      if (enclosingClass != null) {
        DependentLink thisParam = createThisParam(enclosingClass, new LevelArguments(Level.map(typedDef.getEnclosingPolyParams())));
        context.add(thisParam);
        context.addAll(typedDef.getPolyParams());
        visitor.setThisClass(enclosingClass, Reference(thisParam));
      }

      for (Abstract.SuperClass aSuperClass : def.getSuperClasses()) {
        CheckTypeVisitor.Result result = visitor.checkType(aSuperClass.getSuperClass(), null);
        if (result == null) {
          classOk = false;
          continue;
        }

        ClassCallExpression typeCheckedSuperClass = result.getExpression().normalize(NormalizeVisitor.Mode.WHNF).toClassCall();
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

      /* if (enclosingClass != null) {
        assert context.size() == 1;
        context.remove(0);
      } else {
        assert context.size() == 0;
      } /**/
      context.clear();

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

        DependentLink thisParameter = createThisParam(typedDef, new LevelArguments(Level.map(typedDef.getPolyParams())));
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
      typedDef.hasErrors(classOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.HAS_ERRORS);
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

    DependentLink thisParameter = createThisParam(enclosingClass, new LevelArguments(Level.map(enclosingClass.getPolyParams())));
    List<Binding> context = new ArrayList<>();
    context.add(thisParameter);
    context.addAll(enclosingClass.getPolyParams());
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).instancePool(EmptyInstancePool.INSTANCE).thisClass(enclosingClass, Reference(thisParameter)).build();
    ClassField typedDef = new ClassField(def, null, enclosingClass, thisParameter);
    myState.record(def, typedDef);

    CheckTypeVisitor.Result typeResult = visitor.checkType(def.getResultType(), new PiTypeOmega(EmptyDependentLink.getInstance()));
    typedDef.setBaseType(typeResult == null ? Error(null, null) : typeResult.getExpression());
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

    List<Binding> context = new ArrayList<>();
    LinkList list = new LinkList();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, myStaticNsProvider, myDynamicNsProvider, context, myErrorReporter).build();

    List<TypedBinding> polyParamsList = new ArrayList<>();
    boolean paramsOk = visitParameters(def.getArguments(), def, context, polyParamsList, list, visitor, null);
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
