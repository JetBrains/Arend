package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.StripVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
import com.jetbrains.jetpad.vclang.core.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.PatternsToElimTreeConversion;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ConditionsChecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ElimTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.PatternTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.CompositeInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.LocalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CollectDefCallsVisitor;
import com.jetbrains.jetpad.vclang.typechecking.visitor.FindMatchOnIntervalVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.FieldCall;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.parameter;
import static com.jetbrains.jetpad.vclang.core.pattern.Utils.processImplicit;
import static com.jetbrains.jetpad.vclang.term.Util.getReferableList;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

class DefinitionTypechecking {
  static Definition typeCheckHeader(CheckTypeVisitor visitor, GlobalInstancePool instancePool, Abstract.Definition definition, Abstract.ClassDefinition enclosingClass) {
    LocalInstancePool localInstancePool = new LocalInstancePool();
    visitor.setClassViewInstancePool(new CompositeInstancePool(localInstancePool, instancePool));
    ClassDefinition typedEnclosingClass = enclosingClass == null ? null : (ClassDefinition) visitor.getTypecheckingState().getTypechecked(enclosingClass);
    Definition typechecked = visitor.getTypecheckingState().getTypechecked(definition);

    if (definition instanceof Abstract.FunctionDefinition) {
      FunctionDefinition functionDef = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(definition);
      typeCheckFunctionHeader(functionDef, typedEnclosingClass, visitor, localInstancePool);
      if (functionDef.getResultType() == null) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Cannot infer the result type of a recursive function", definition));
        functionDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      }
      return functionDef;
    } else
    if (definition instanceof Abstract.DataDefinition) {
      DataDefinition dataDef = typechecked != null ? (DataDefinition) typechecked : new DataDefinition((Abstract.DataDefinition) definition);
      typeCheckDataHeader(dataDef, typedEnclosingClass, visitor, localInstancePool);
      if (dataDef.getSort() == null || dataDef.getSort().getPLevel().isInfinity()) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Cannot infer the sort of a recursive data type", definition));
        dataDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      }
      return dataDef;
    } else {
      throw new IllegalStateException();
    }
  }

  static void typeCheckBody(Definition definition, CheckTypeVisitor exprVisitor, Set<DataDefinition> dataDefinitions) {
    if (definition instanceof FunctionDefinition) {
      typeCheckFunctionBody((FunctionDefinition) definition, exprVisitor);
    } else
    if (definition instanceof DataDefinition) {
      if (!typeCheckDataBody((DataDefinition) definition, exprVisitor, false, dataDefinitions)) {
        definition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      }
    } else {
      throw new IllegalStateException();
    }
  }

  static Definition typeCheck(TypecheckerState state, GlobalInstancePool instancePool, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, TypecheckingUnit unit, boolean recursive, LocalErrorReporter errorReporter) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(state, staticNsProvider, dynamicNsProvider, new LinkedHashMap<>(), errorReporter, instancePool);
    ClassDefinition enclosingClass = unit.getEnclosingClass() == null ? null : (ClassDefinition) state.getTypechecked(unit.getEnclosingClass());
    Definition typechecked = state.getTypechecked(unit.getDefinition());

    if (unit.getDefinition() instanceof Abstract.ClassDefinition) {
      ClassDefinition definition = typechecked != null ? (ClassDefinition) typechecked : new ClassDefinition((Abstract.ClassDefinition) unit.getDefinition());
      typeCheckClass(definition, enclosingClass, visitor);
      return definition;
    } else
    if (unit.getDefinition() instanceof Abstract.ClassViewInstance) {
      FunctionDefinition definition = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(unit.getDefinition());
      typeCheckClassViewInstance(definition, visitor);
      return definition;
    }

    LocalInstancePool localInstancePool = new LocalInstancePool();
    visitor.setClassViewInstancePool(new CompositeInstancePool(localInstancePool, instancePool));

    if (unit.getDefinition() instanceof Abstract.FunctionDefinition) {
      FunctionDefinition definition = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(unit.getDefinition());
      typeCheckFunctionHeader(definition, enclosingClass, visitor, localInstancePool);
      if (definition.getResultType() == null) {
        definition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
        if (recursive) {
          if (((Abstract.FunctionDefinition) unit.getDefinition()).getResultType() == null) {
            errorReporter.report(new LocalTypeCheckingError("Cannot infer the result type of a recursive function", unit.getDefinition()));
          }
          return definition;
        }
      }
      typeCheckFunctionBody(definition, visitor);
      return definition;
    } else
    if (unit.getDefinition() instanceof Abstract.DataDefinition) {
      DataDefinition definition = typechecked != null ? (DataDefinition) typechecked : new DataDefinition((Abstract.DataDefinition) unit.getDefinition());
      typeCheckDataHeader(definition, enclosingClass, visitor, localInstancePool);
      if (definition.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
        typeCheckDataBody(definition, visitor, true, Collections.singleton(definition));
      }
      return definition;
    } else {
      throw new IllegalStateException();
    }
  }

  private static TypedDependentLink createThisParam(ClassDefinition enclosingClass) {
    assert enclosingClass != null;
    return parameter("\\this", new ClassCallExpression(enclosingClass, Sort.STD));
  }

  private static boolean typeCheckParameters(List<? extends Abstract.Argument> arguments, LinkList list, CheckTypeVisitor visitor, LocalInstancePool localInstancePool, Map<Integer, ClassField> classifyingFields) {
    boolean ok = true;
    int index = 0;

    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) argument;
        Type paramResult = visitor.finalCheckType(typeArgument.getType());
        if (paramResult == null) {
          ok = false;
          paramResult = new TypeExpression(new ErrorExpression(null, null), Sort.SET0);
        }

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          List<? extends Abstract.ReferableSourceNode> referableList = ((Abstract.TelescopeArgument) argument).getReferableList();
          List<String> names = referableList.stream().map(r -> r == null ? null : r.getName()).collect(Collectors.toList());
          param = parameter(argument.getExplicit(), names, paramResult);
          index += names.size();

          int i = 0;
          for (DependentLink link = param; link.hasNext(); link = link.getNext(), i++) {
            visitor.getContext().put(referableList.get(i), link);
          }
        } else {
          param = parameter(argument.getExplicit(), (String) null, paramResult);
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
        visitor.getFreeBindings().addAll(toContext(param));
      } else {
        visitor.getErrorReporter().report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0]));
        ok = false;
      }
    }

    return ok;
  }

  private static LinkList initializeThisParam(CheckTypeVisitor visitor, ClassDefinition enclosingClass) {
    LinkList list = new LinkList();
    if (enclosingClass != null) {
      DependentLink thisParam = createThisParam(enclosingClass);
      visitor.getFreeBindings().add(thisParam);
      list.append(thisParam);
      visitor.setThis(enclosingClass, thisParam);
    }
    return list;
  }

  private static void typeCheckFunctionHeader(FunctionDefinition typedDef, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    LinkList list = initializeThisParam(visitor, enclosingClass);

    Map<Integer, ClassField> classifyingFields = new HashMap<>();
    Abstract.FunctionDefinition def = (Abstract.FunctionDefinition) typedDef.getAbstractDefinition();
    boolean paramsOk = typeCheckParameters(def.getArguments(), list, visitor, localInstancePool, classifyingFields);
    Expression expectedType = null;
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      Type expectedTypeResult = visitor.finalCheckType(resultType);
      if (expectedTypeResult != null) {
        expectedType = expectedTypeResult.getExpr();
      }
    }

    visitor.getTypecheckingState().record(def, typedDef);
    typedDef.setClassifyingFieldsOfParameters(classifyingFields);
    typedDef.setThisClass(enclosingClass);
    typedDef.setParameters(list.getFirst());
    typedDef.setResultType(expectedType);
    typedDef.setStatus(paramsOk ? Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING : Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
  }

  private static void typeCheckFunctionBody(FunctionDefinition typedDef, CheckTypeVisitor visitor) {
    Abstract.FunctionDefinition def = (Abstract.FunctionDefinition) typedDef.getAbstractDefinition();
    Abstract.FunctionBody body = def.getBody();

    if (body != null) {
      Expression expectedType = typedDef.getResultType();

      if (body instanceof Abstract.ElimFunctionBody) {
        if (expectedType != null) {
          ElimTree elimTree = new ElimTypechecking(visitor, expectedType, false).typecheckElim(((Abstract.ElimFunctionBody) body), typedDef.getParameters());
          if (elimTree != null) {
            typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
            if (ConditionsChecking.check(elimTree)) {
              typedDef.setElimTree(elimTree);
            }
          }
        } else {
          if (def.getResultType() == null) {
            visitor.getErrorReporter().report(new LocalTypeCheckingError("Cannot infer type of the expression", body));
          }
        }
      } else {
        CheckTypeVisitor.Result termResult = visitor.finalCheckExpr(((Abstract.TermFunctionBody) body).getTerm(), expectedType);
        if (termResult != null) {
          typedDef.setElimTree(new LeafElimTree(typedDef.getParameters(), termResult.expression));
          if (expectedType == null) {
            typedDef.setResultType(termResult.type);
          }
        }
      }
    }

    typedDef.setStatus(typedDef.getResultType() == null ? Definition.TypeCheckingStatus.HEADER_HAS_ERRORS : typedDef.getElimTree() == null ? Definition.TypeCheckingStatus.BODY_HAS_ERRORS : Definition.TypeCheckingStatus.NO_ERRORS);
  }

  private static void typeCheckDataHeader(DataDefinition dataDefinition, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    LinkList list = initializeThisParam(visitor, enclosingClass);

    Map<Integer, ClassField> classifyingFields = new HashMap<>();
    Sort userSort = null;
    boolean paramsOk;
    Abstract.DataDefinition def = dataDefinition.getAbstractDefinition();
    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(visitor.getContext())) {
      paramsOk = typeCheckParameters(def.getParameters(), list, visitor, localInstancePool, classifyingFields);
    }

    if (def.getUniverse() != null) {
      if (def.getUniverse() instanceof Abstract.UniverseExpression) {
        Type userTypeResult = visitor.finalCheckType(def.getUniverse());
        if (userTypeResult != null) {
          userSort = userTypeResult.getExpr().toSort();
          if (userSort == null) {
            visitor.getErrorReporter().report(new LocalTypeCheckingError("Expected a universe", def.getUniverse()));
          }
        }
      } else {
        String msg = "Specified type " + PrettyPrintVisitor.prettyPrint(def.getUniverse(), 0) + " of '" + def.getName() + "' is not a universe";
        visitor.getErrorReporter().report(new LocalTypeCheckingError(msg, def.getUniverse()));
      }
    }

    dataDefinition.setClassifyingFieldsOfParameters(classifyingFields);
    dataDefinition.setThisClass(enclosingClass);
    dataDefinition.setParameters(list.getFirst());
    dataDefinition.setSort(userSort);
    visitor.getTypecheckingState().record(def, dataDefinition);

    if (!paramsOk) {
      dataDefinition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      for (Abstract.Constructor constructor : def.getConstructors()) {
        visitor.getTypecheckingState().record(constructor, new Constructor(constructor, dataDefinition));
      }
    } else {
      dataDefinition.setStatus(Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING);
    }
  }

  private static boolean typeCheckDataBody(DataDefinition dataDefinition, CheckTypeVisitor visitor, boolean polyHLevel, Set<DataDefinition> dataDefinitions) {
    Abstract.DataDefinition def = dataDefinition.getAbstractDefinition();
    Sort userSort = dataDefinition.getSort();
    Sort inferredSort = Sort.PROP;
    if (userSort != null) {
      if (!userSort.getPLevel().isInfinity()) {
        inferredSort = inferredSort.max(new Sort(userSort.getPLevel(), inferredSort.getHLevel()));
      }
      if (!polyHLevel || !userSort.getHLevel().isInfinity()) {
        inferredSort = inferredSort.max(new Sort(inferredSort.getPLevel(), userSort.getHLevel()));
      }
    }
    dataDefinition.setSort(inferredSort);
    if (def.getConstructors().size() > 1) {
      inferredSort = inferredSort.max(Sort.SET0);
    }

    boolean dataOk = true;
    List<DependentLink> elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getConstructors(), dataDefinition.getParameters(), visitor);
    if (elimParams == null) {
      dataOk = false;
    }

    boolean universeOk = true;
    if (dataOk) {
      for (Abstract.Constructor constructor : def.getConstructors()) {
        Sort conSort = typeCheckConstructor(constructor, elimParams, dataDefinition, visitor, dataDefinitions);
        if (conSort == null) {
          dataOk = false;
          conSort = Sort.PROP;
        }

        inferredSort = inferredSort.max(conSort);
        if (userSort != null) {
          if (!def.isTruncated() && !conSort.isLessOrEquals(userSort)) {
            String msg = "Universe " + conSort + " of constructor '" + constructor.getName() + "' is not compatible with expected universe " + userSort;
            visitor.getErrorReporter().report(new LocalTypeCheckingError(msg, constructor));
            universeOk = false;
          }
        }
      }
    }
    dataDefinition.setStatus(dataOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.BODY_HAS_ERRORS);

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

      for (Constructor constructor : dataDefinition.getConstructors()) {
        if (constructor.getCondition() != null) {
          LocalTypeCheckingError error = TypeCheckingElim.checkConditions(constructor.getName(), def, constructor.getParameters(), constructor.getCondition());
          if (error != null) {
            visitor.getErrorReporter().report(error);
            constructor.setCondition(null);
            dataDefinition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
          } else {
            if (!dataDefinition.matchesOnInterval() && constructor.getCondition().accept(new FindMatchOnIntervalVisitor(), null)) {
              dataDefinition.setMatchesOnInterval();
              inferredSort = inferredSort.max(new Sort(inferredSort.getPLevel(), Level.INFINITY));
            }
          }
        }
      }
    }

    int index = 0;
    for (DependentLink link = dataDefinition.getParameters(); link.hasNext(); link = link.getNext(), index++) {
      boolean isCovariant = true;
      for (Constructor constructor : dataDefinition.getConstructors()) {
        for (DependentLink link1 = constructor.getParameters(); link1.hasNext(); link1 = link1.getNext()) {
          link1 = link1.getNextTyped(null);
          if (!checkPositiveness(link1.getType().getExpr(), index, null, null, null, Collections.singleton(link))) {
            isCovariant = false;
            break;
          }
        }
        if (!isCovariant) {
          break;
        }
      }
      if (isCovariant) {
        dataDefinition.setCovariant(index);
      }
    }

    if (def.isTruncated()) {
      if (userSort == null) {
        String msg = "The data type cannot be truncated since its universe is not specified";
        visitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, msg, def));
      } else {
        if (inferredSort.isLessOrEquals(userSort)) {
          String msg = "The data type will not be truncated since it already fits in the specified universe";
          visitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, msg, def.getUniverse()));
        } else {
          dataDefinition.setIsTruncated(true);
        }
      }
    } else if (universeOk && userSort != null && !inferredSort.isLessOrEquals(userSort)) {
      String msg = "Actual universe " + inferredSort + " is not compatible with expected universe " + userSort;
      visitor.getErrorReporter().report(new LocalTypeCheckingError(msg, def.getUniverse()));
      universeOk = false;
    }

    dataDefinition.setSort(universeOk && userSort != null ? userSort : inferredSort);
    return universeOk;
  }

  private static List<Constructor> typeCheckConditions(CheckTypeVisitor visitor, DataDefinition dataDefinition, Abstract.DataDefinition def) {
    Map<Constructor, List<Abstract.Condition>> condMap = new HashMap<>();
    for (Abstract.Condition cond : def.getConditions()) {
      Constructor constructor = dataDefinition.getConstructor(cond.getConstructorName());
      if (constructor == null) {
        visitor.getErrorReporter().report(new NotInScopeError(def, cond.getConstructorName()));  // TODO: refer by reference
        continue;
      }
      if (!constructor.status().headerIsOK()) {
        continue;
      }
      if (!condMap.containsKey(constructor)) {
        condMap.put(constructor, new ArrayList<>());
      }
      condMap.get(constructor).add(cond);
    }
    List<Constructor> cycle = searchConditionCycle(condMap, visitor.getTypecheckingState());
    if (cycle != null) {
      return cycle;
    }
    for (Constructor constructor : condMap.keySet()) {
      try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(visitor.getContext())) {
        List<List<Pattern>> patterns = new ArrayList<>();
        List<Expression> expressions = new ArrayList<>();
        visitor.getFreeBindings().addAll(toContext(constructor.getDataTypeParameters()));
        // visitor.getContext().addAll(toContext(constructor.getDataTypeParameters())); // TODO[context]

        for (Abstract.Condition cond : condMap.get(constructor)) {
          try (Utils.SetContextSaver saver = new Utils.SetContextSaver<>(visitor.getContext())) {
            List<Expression> resultType = new ArrayList<>(Collections.singletonList(constructor.getDataTypeExpression(Sort.STD)));
            DependentLink params = constructor.getParameters();
            List<Abstract.Pattern> processedPatterns = processImplicitPatterns(cond, params, cond.getPatterns(), visitor.getErrorReporter());
            if (processedPatterns == null)
              continue;

            List<Abstract.ReferableSourceNode> referableList = new ArrayList<>();
            getReferableList(constructor.getAbstractDefinition().getArguments(), referableList);
            Patterns typedPatterns = visitor.getTypeCheckingElim().visitPatternArgs(processedPatterns, referableList, constructor.getParameters(), resultType, TypeCheckingElim.PatternExpansionMode.CONDITION);
            if (typedPatterns == null) {
              continue;
            }

            CheckTypeVisitor.Result result = visitor.finalCheckExpr(cond.getTerm(), resultType.get(0));
            if (result == null)
              continue;

            patterns.add(typedPatterns.getPatterns());
            expressions.add(result.expression.normalize(NormalizeVisitor.Mode.NF));
          }
        }

        PatternsToElimTreeConversion.OKResult elimTreeResult = (PatternsToElimTreeConversion.OKResult) PatternsToElimTreeConversion.convert(constructor.getParameters(), patterns, expressions);

        if (!elimTreeResult.elimTree.accept(new TerminationCheckVisitor(constructor, constructor.getDataTypeParameters(), constructor.getParameters()), null)) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Termination check failed", null));
          continue;
        }

        constructor.setCondition(elimTreeResult.elimTree);
        for (Abstract.Condition cond : condMap.get(constructor)) {
          cond.setWellTyped(elimTreeResult.elimTree);
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

  private static Sort typeCheckConstructor(Abstract.Constructor def, List<DependentLink> elimParams, DataDefinition dataDefinition, CheckTypeVisitor visitor, Set<DataDefinition> dataDefinitions) {
    Sort sort = Sort.PROP;
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(visitor.getContext())) {
      List<? extends Abstract.TypeArgument> arguments = def.getArguments();
      Constructor constructor = new Constructor(def, dataDefinition);
      visitor.getTypecheckingState().record(def, constructor);
      List<? extends Abstract.Pattern> patterns = def.getPatterns();
      com.jetbrains.jetpad.vclang.core.elimtree.Patterns typedPatterns = null;
      if (patterns != null) {
        List<Abstract.Pattern> processedPatterns = new ArrayList<>(patterns);
        if (dataDefinition.getThisClass() != null) {
          processedPatterns.add(0, new NamePattern(dataDefinition.getParameters()));
        }
        processedPatterns = processImplicitPatterns(def, dataDefinition.getParameters(), processedPatterns, visitor.getErrorReporter());
        if (processedPatterns == null) {
          return null;
        }

        List<Abstract.ReferableSourceNode> referableList = new ArrayList<>();
        if (dataDefinition.getThisClass() != null) {
          referableList.add(null);
        }
        getReferableList(dataDefinition.getAbstractDefinition().getParameters(), referableList);
        Pair<List<com.jetbrains.jetpad.vclang.core.elimtree.Pattern>, List<Expression>> result = new PatternTypechecking(visitor.getErrorReporter(), false).typecheckPatterns(def.getPatterns(), elimParams, dataDefinition.getParameters(), def, visitor);
        if (result == null) {
          return null;
        }
        typedPatterns = new com.jetbrains.jetpad.vclang.core.elimtree.Patterns(result.proj1);
      } else {
        List<Abstract.ReferableSourceNode> referableList = new ArrayList<>();
        if (dataDefinition.getThisClass() != null) {
          referableList.add(null);
        }
        getReferableList(dataDefinition.getAbstractDefinition().getParameters(), referableList);
        int i = 0;
        for (DependentLink link = dataDefinition.getParameters(); link.hasNext(); link = link.getNext(), i++) {
          if (referableList.get(i) != null) {
            visitor.getContext().put(referableList.get(i), link);
          }
        }
        visitor.getFreeBindings().addAll(toContext(dataDefinition.getParameters()));
      }

      if (dataDefinition.getThisClass() != null && typedPatterns != null) {
        visitor.setThis(dataDefinition.getThisClass(), typedPatterns.getFirstBinding());
      }

      LinkList list = new LinkList();
      for (Abstract.TypeArgument argument : arguments) {
        Type paramResult = visitor.finalCheckType(argument.getType());
        if (paramResult == null) {
          return null;
        }

        sort = sort.max(paramResult.getSortOfType());

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          List<? extends Abstract.ReferableSourceNode> referableList = ((Abstract.TelescopeArgument) argument).getReferableList();
          param = parameter(argument.getExplicit(), referableList.stream().map(r -> r == null ? null : r.getName()).collect(Collectors.toList()), paramResult);
          int i = 0;
          for (DependentLink link = param; link.hasNext(); link = link.getNext(), i++) {
            visitor.getContext().put(referableList.get(i), link);
          }
        } else {
          param = parameter(argument.getExplicit(), (String) null, paramResult);
        }
        list.append(param);
        visitor.getFreeBindings().addAll(toContext(param));
      }

      int index = 0;
      for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext(), index++) {
        link = link.getNextTyped(null);
        if (!checkPositiveness(link.getType().getExpr(), index, arguments, def, visitor.getErrorReporter(), dataDefinitions)) {
          return null;
        }
      }

      constructor.setParameters(list.getFirst());
      constructor.setPatterns(typedPatterns);
      constructor.setThisClass(dataDefinition.getThisClass());
      constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
      dataDefinition.addConstructor(constructor);
      return sort;
    }
  }

  private static boolean checkPositiveness(Expression type, int index, List<? extends Abstract.Argument> arguments, Abstract.Constructor constructor, LocalErrorReporter errorReporter, Set<? extends Variable> variables) {
    List<SingleDependentLink> piParams = new ArrayList<>();
    type = type.getPiParameters(piParams, true, false);
    for (DependentLink piParam : piParams) {
      if (piParam instanceof UntypedDependentLink) {
        continue;
      }
      if (!checkNonPositiveError(piParam.getType().getExpr(), index, arguments, constructor, errorReporter, variables)) {
        return false;
      }
    }

    if (type.toDataCall() != null) {
      List<? extends Expression> exprs = type.toDataCall().getDefCallArguments();
      DataDefinition typeDef = type.toDataCall().getDefinition();

      for (int i = 0; i < exprs.size(); i++) {
        if (typeDef.isCovariant(i)) {
          Expression expr = exprs.get(i).normalize(NormalizeVisitor.Mode.WHNF);
          while (expr.toLam() != null) {
            expr = expr.toLam().getBody().normalize(NormalizeVisitor.Mode.WHNF);
          }
          if (!checkPositiveness(expr, index, arguments, constructor, errorReporter, variables)) {
            return false;
          }
        } else {
          if (!checkNonPositiveError(exprs.get(i), index, arguments, constructor, errorReporter, variables)) {
            return false;
          }
        }
      }
    } else if (type.toApp() != null) {
      for (; type.toApp() != null; type = type.toApp().getFunction()) {
        if (!checkNonPositiveError(type.toApp().getArgument(), index, arguments, constructor, errorReporter, variables)) {
          return false;
        }
      }
      if (type.toReference() == null) {
        if (!checkNonPositiveError(type, index, arguments, constructor, errorReporter, variables)) {
          return false;
        }
      }
    } else {
      if (!checkNonPositiveError(type, index, arguments, constructor, errorReporter, variables)) {
        return false;
      }
    }

    return true;
  }

  private static boolean checkNonPositiveError(Expression expr, int index, List<? extends Abstract.Argument> args, Abstract.Constructor constructor, LocalErrorReporter errorReporter, Set<? extends Variable> variables) {
    Variable def = expr.findBinding(variables);
    if (def == null) {
      return true;
    }

    if (errorReporter == null) {
      return false;
    }

    int i = 0;
    Abstract.Argument argument = null;
    for (Abstract.Argument arg : args) {
      if (arg instanceof Abstract.TelescopeArgument) {
        i += ((Abstract.TelescopeArgument) arg).getReferableList().size();
      } else {
        i++;
      }
      if (i > index) {
        argument = arg;
        break;
      }
    }

    String msg = "Non-positive recursive occurrence of data type " + ((DataDefinition) def).getName() + " in constructor " + constructor.getName();
    errorReporter.report(new LocalTypeCheckingError(msg, argument == null ? constructor : argument));
    return false;
  }

  private static List<Abstract.Pattern> processImplicitPatterns(Abstract.SourceNode expression, DependentLink parameters, List<? extends Abstract.Pattern> patterns, LocalErrorReporter errorReporter) {
    List<Abstract.Pattern> processedPatterns = null;
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

  private static void typeCheckClass(ClassDefinition typedDef, ClassDefinition enclosingClass, CheckTypeVisitor visitor) {
    LocalErrorReporter errorReporter = visitor.getErrorReporter();
    boolean classOk = true;

    FieldSet fieldSet = new FieldSet(Sort.PROP);
    Set<ClassDefinition> superClasses = new HashSet<>();
    Abstract.ClassDefinition def = typedDef.getAbstractDefinition();
    visitor.getTypecheckingState().record(def, typedDef);

    try {
      typedDef.setFieldSet(fieldSet);
      typedDef.setSuperClasses(superClasses);
      typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
      typedDef.setThisClass(enclosingClass);

      if (enclosingClass != null) {
        DependentLink thisParam = createThisParam(enclosingClass);
        visitor.getFreeBindings().add(thisParam);
        visitor.setThis(enclosingClass, thisParam);
      }

      for (Abstract.SuperClass aSuperClass : def.getSuperClasses()) {
        CheckTypeVisitor.Result result = visitor.finalCheckExpr(aSuperClass.getSuperClass(), null);
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
          if (oldImpl == null || oldImpl.substThisParam(new ReferenceExpression(entry.getValue().thisParam)).equals(entry.getValue().term)) {
            fieldSet.implementField(entry.getKey(), entry.getValue());
          } else {
            classOk = false;
            errorReporter.report(new LocalTypeCheckingError("Implementations of '" + entry.getKey().getName() + "' differ", aSuperClass.getSuperClass()));
          }
        }
      }

      for (Abstract.ClassField field : def.getFields()) {
        typeCheckClassField(field, typedDef, fieldSet, visitor);
      }

      if (!def.getImplementations().isEmpty()) {
        typedDef.updateSorts();
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

          TypedDependentLink thisParameter = createThisParam(typedDef);
          visitor.getFreeBindings().add(thisParameter);
          visitor.setThis(typedDef, thisParameter);
          CheckTypeVisitor.Result result = implementField(fieldSet, field, implementation.getImplementation(), visitor, thisParameter);
          if (result == null || result.expression.toError() != null) {
            classOk = false;
          }
        }
      }

      if (classOk) {
        typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
      }
    } catch (Namespace.InvalidNamespaceException e) {
      errorReporter.report(e.toError());
    } finally {
      typedDef.updateSorts();
    }
  }

  private static CheckTypeVisitor.Result implementField(FieldSet fieldSet, ClassField field, Abstract.Expression implBody, CheckTypeVisitor visitor, TypedDependentLink thisParam) {
    CheckTypeVisitor.Result result = visitor.finalCheckExpr(implBody, field.getBaseType(Sort.STD).subst(field.getThisParameter(), new ReferenceExpression(thisParam)));
    fieldSet.implementField(field, new FieldSet.Implementation(thisParam, result != null ? result.expression : new ErrorExpression(null, null)));
    return result;
  }

  private static ClassField typeCheckClassField(Abstract.ClassField def, ClassDefinition enclosingClass, FieldSet fieldSet, CheckTypeVisitor visitor) {
    TypedDependentLink thisParameter = createThisParam(enclosingClass);
    visitor.getFreeBindings().add(thisParameter);
    visitor.setThis(enclosingClass, thisParameter);
    Type typeResult = visitor.finalCheckType(def.getResultType());

    ClassField typedDef = new ClassField(def, typeResult == null ? new ErrorExpression(null, null) : typeResult.getExpr(), enclosingClass, thisParameter);
    if (typeResult == null) {
      typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
    }
    visitor.getTypecheckingState().record(def, typedDef);
    fieldSet.addField(typedDef);
    return typedDef;
  }

  private static void typeCheckClassViewInstance(FunctionDefinition typedDef, CheckTypeVisitor visitor) {
    LocalErrorReporter errorReporter = visitor.getErrorReporter();
    TypecheckerState state = visitor.getTypecheckingState();

    LinkList list = new LinkList();
    Abstract.ClassViewInstance def = (Abstract.ClassViewInstance) typedDef.getAbstractDefinition();
    boolean paramsOk = typeCheckParameters(def.getArguments(), list, visitor, null, null);
    typedDef.setParameters(list.getFirst());
    typedDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
    state.record(def, typedDef);
    if (!paramsOk) {
      return;
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

    FieldSet fieldSet = new FieldSet(Sort.PROP);
    ClassDefinition classDef = (ClassDefinition) visitor.getTypecheckingState().getTypechecked((Abstract.Definition) classView.getUnderlyingClassReference().getReferent());
    fieldSet.addFieldsFrom(classDef.getFieldSet());
    ClassCallExpression term = new ClassCallExpression(classDef, Sort.generateInferVars(visitor.getEquations(), def.getClassView()), fieldSet);

    for (ClassField field : classDef.getFieldSet().getFields()) {
      Abstract.ClassFieldImpl impl = classFieldMap.get(field);
      if (impl != null) {
        visitor.implementField(fieldSet, field, impl.getImplementation(), term);
        classFieldMap.remove(field);
      } else {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Field '" + field.getName() + "' is not implemented", def));
        return;
      }
    }

    LevelSubstitution substitution = visitor.getEquations().solve(def);
    if (!substitution.isEmpty()) {
      term = new SubstVisitor(new ExprSubstitution(), substitution).visitClassCall(term, null);
    }
    term = new StripVisitor(new HashSet<>(visitor.getFreeBindings()), visitor.getErrorReporter()).visitClassCall(term, null);

    FieldSet.Implementation impl = fieldSet.getImplementation((ClassField) state.getTypechecked(classView.getClassifyingField()));
    DefCallExpression defCall = impl.term.normalize(NormalizeVisitor.Mode.WHNF).toDefCall();
    if (defCall == null || !defCall.getDefCallArguments().isEmpty()) {
      errorReporter.report(new LocalTypeCheckingError("Expected a definition in the classifying field", def));
      return;
    }

    typedDef.setResultType(term);
    typedDef.setElimTree(new LeafElimTree(list.getFirst(), new NewExpression(term)));
    typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
  }
}
