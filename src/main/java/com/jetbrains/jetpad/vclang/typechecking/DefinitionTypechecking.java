package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.Body;
import com.jetbrains.jetpad.vclang.core.elimtree.Clause;
import com.jetbrains.jetpad.vclang.core.elimtree.IntervalElim;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporterCounter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ConditionsChecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ElimTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.PatternTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.CompositeInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.LocalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.FieldCall;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.parameter;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

// TODO[abstract]: Make this class into a visitor
class DefinitionTypechecking {
  static Definition typecheckHeader(CheckTypeVisitor visitor, GlobalInstancePool instancePool, Concrete.Definition definition, Concrete.ClassDefinition enclosingClass) {
    LocalInstancePool localInstancePool = new LocalInstancePool();
    visitor.setInstancePool(new CompositeInstancePool(localInstancePool, instancePool));
    ClassDefinition typedEnclosingClass = enclosingClass == null ? null : (ClassDefinition) visitor.getTypecheckingState().getTypechecked(enclosingClass.getReferable());
    Definition typechecked = visitor.getTypecheckingState().getTypechecked(definition.getReferable());

    if (definition instanceof Concrete.FunctionDefinition) {
      FunctionDefinition functionDef = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(definition.getReferable());
      typeCheckFunctionHeader(functionDef, (Concrete.FunctionDefinition) definition, typedEnclosingClass, visitor, localInstancePool);
      if (functionDef.getResultType() == null) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Cannot infer the result type of a recursive function", definition));
        functionDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      }
      return functionDef;
    } else
    if (definition instanceof Concrete.DataDefinition) {
      DataDefinition dataDef = typechecked != null ? (DataDefinition) typechecked : new DataDefinition(definition.getReferable());
      typeCheckDataHeader(dataDef, (Concrete.DataDefinition) definition, typedEnclosingClass, visitor, localInstancePool);
      if (dataDef.getSort() == null || dataDef.getSort().getPLevel().isInfinity()) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Cannot infer the sort of a recursive data type", definition));
        dataDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      }
      return dataDef;
    } else {
      throw new IllegalStateException();
    }
  }

  static List<Clause> typecheckBody(Definition definition, Concrete.Definition def, CheckTypeVisitor exprVisitor, Set<DataDefinition> dataDefinitions) {
    if (definition instanceof FunctionDefinition) {
      return typeCheckFunctionBody((FunctionDefinition) definition, (Concrete.FunctionDefinition) def, exprVisitor);
    } else
    if (definition instanceof DataDefinition) {
      if (!typeCheckDataBody((DataDefinition) definition, (Concrete.DataDefinition) def, exprVisitor, false, dataDefinitions)) {
        definition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      }
    } else {
      throw new IllegalStateException();
    }
    return null;
  }

  static List<Clause> typecheck(TypecheckerState state, GlobalInstancePool instancePool, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, TypecheckingUnit unit, boolean recursive, LocalErrorReporter errorReporter) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(state, staticNsProvider, dynamicNsProvider, new LinkedHashMap<>(), errorReporter, instancePool);
    ClassDefinition enclosingClass = unit.getEnclosingClass() == null ? null : (ClassDefinition) state.getTypechecked(unit.getEnclosingClass().getReferable());
    Definition typechecked = state.getTypechecked(unit.getDefinition().getReferable());

    if (unit.getDefinition() instanceof Concrete.ClassDefinition) {
      ClassDefinition definition = typechecked != null ? (ClassDefinition) typechecked : new ClassDefinition(unit.getDefinition().getReferable());
      if (typechecked == null) {
        state.record(unit.getDefinition().getReferable(), definition);
      }
      if (recursive) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("A class cannot be recursive", unit.getDefinition()));
        definition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);

        for (Concrete.ClassField field : ((Concrete.ClassDefinition) unit.getDefinition()).getFields()) {
          ClassField typedDef = new ClassField(field.getReferable(), new ErrorExpression(null, null), definition, createThisParam(definition));
          typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
          visitor.getTypecheckingState().record(field.getReferable(), typedDef);
          definition.addField(typedDef);
          definition.addPersonalField(typedDef);
        }
      } else {
        typeCheckClass((Concrete.ClassDefinition) unit.getDefinition(), definition, enclosingClass, visitor);
      }
      return null;
    } else
    if (unit.getDefinition() instanceof Concrete.Instance) {
      FunctionDefinition definition = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(unit.getDefinition().getReferable());
      if (typechecked == null) {
        state.record(unit.getDefinition().getReferable(), definition);
      }
      if (recursive) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("An instance cannot be recursive", unit.getDefinition()));
        definition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
      } else {
        typeCheckInstance((Concrete.Instance) unit.getDefinition(), definition, visitor);
      }
      return null;
    }

    LocalInstancePool localInstancePool = new LocalInstancePool();
    visitor.setInstancePool(new CompositeInstancePool(localInstancePool, instancePool));

    if (unit.getDefinition() instanceof Concrete.FunctionDefinition) {
      FunctionDefinition definition = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(unit.getDefinition().getReferable());
      typeCheckFunctionHeader(definition, (Concrete.FunctionDefinition) unit.getDefinition(), enclosingClass, visitor, localInstancePool);
      if (definition.getResultType() == null) {
        definition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
        if (recursive) {
          if (((Concrete.FunctionDefinition) unit.getDefinition()).getResultType() == null) {
            errorReporter.report(new LocalTypeCheckingError("Cannot infer the result type of a recursive function", unit.getDefinition()));
          }
          return null;
        }
      }
      return typeCheckFunctionBody(definition, (Concrete.FunctionDefinition) unit.getDefinition(), visitor);
    } else
    if (unit.getDefinition() instanceof Concrete.DataDefinition) {
      DataDefinition definition = typechecked != null ? (DataDefinition) typechecked : new DataDefinition(unit.getDefinition().getReferable());
      typeCheckDataHeader(definition, (Concrete.DataDefinition) unit.getDefinition(), enclosingClass, visitor, localInstancePool);
      if (definition.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
        typeCheckDataBody(definition, (Concrete.DataDefinition) unit.getDefinition(), visitor, true, Collections.singleton(definition));
      }
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  private static TypedDependentLink createThisParam(ClassDefinition enclosingClass) {
    assert enclosingClass != null;
    return parameter("\\this", new ClassCallExpression(enclosingClass, Sort.STD));
  }

  private static Sort typeCheckParameters(List<? extends Concrete.Parameter> parameters, LinkList list, CheckTypeVisitor visitor, LocalInstancePool localInstancePool, Sort expectedSort) {
    Sort sort = Sort.PROP;
    int index = 0;

    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TypeParameter) {
        Concrete.TypeParameter typeParameter = (Concrete.TypeParameter) parameter;
        Type paramResult = visitor.finalCheckType(typeParameter.getType(), expectedSort == null ? ExpectedType.OMEGA : new UniverseExpression(expectedSort));
        if (paramResult == null) {
          sort = null;
          paramResult = new TypeExpression(new ErrorExpression(null, null), Sort.SET0);
        } else if (sort != null) {
          sort = sort.max(paramResult.getSortOfType());
        }

        DependentLink param;
        if (parameter instanceof Concrete.TelescopeParameter) {
          List<? extends Referable> referableList = ((Concrete.TelescopeParameter) parameter).getReferableList();
          List<String> names = new ArrayList<>(referableList.size());
          for (Referable referable : referableList) {
            names.add(referable == null ? null : referable.textRepresentation());
          }
          param = parameter(parameter.getExplicit(), names, paramResult);
          index += names.size();

          int i = 0;
          for (DependentLink link = param; link.hasNext(); link = link.getNext(), i++) {
            visitor.getContext().put(referableList.get(i), link);
          }
        } else {
          param = parameter(parameter.getExplicit(), (String) null, paramResult);
          index++;
        }

        if (localInstancePool != null) {
          Concrete.ClassView classView = Concrete.getUnderlyingClassView(typeParameter.getType());
          if (classView != null && classView.getClassifyingField() instanceof GlobalReferable) {
            ClassField classifyingField = (ClassField) visitor.getTypecheckingState().getTypechecked((GlobalReferable) classView.getClassifyingField()); // TODO[abstract]: check that it is a field and that it belongs to the class, also check that referable is global
            for (DependentLink link = param; link.hasNext(); link = link.getNext()) {
              ReferenceExpression reference = new ReferenceExpression(link);
              Expression oldInstance = localInstancePool.addInstance(FieldCall(classifyingField, reference), classView, reference);
              if (oldInstance != null) {
                visitor.getErrorReporter().report(new DuplicateInstanceError(oldInstance, reference, parameter));
              }
            }
          }
        }

        list.append(param);
        for (; param.hasNext(); param = param.getNext()) {
          visitor.getFreeBindings().add(param);
        }
      } else {
        visitor.getErrorReporter().report(new ArgInferenceError(typeOfFunctionArg(++index), parameter, new Expression[0]));
        sort = null;
      }
    }

    return sort;
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

  private static void typeCheckFunctionHeader(FunctionDefinition typedDef, Concrete.FunctionDefinition def, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    LinkList list = initializeThisParam(visitor, enclosingClass);

    boolean paramsOk = typeCheckParameters(def.getParameters(), list, visitor, localInstancePool, null) != null;
    Expression expectedType = null;
    Concrete.Expression resultType = def.getResultType();
    if (resultType != null) {
      Type expectedTypeResult = def.getBody() instanceof Concrete.ElimFunctionBody ? visitor.finalCheckType(resultType, ExpectedType.OMEGA) : visitor.checkType(resultType, ExpectedType.OMEGA);
      if (expectedTypeResult != null) {
        expectedType = expectedTypeResult.getExpr();
      }
    }

    visitor.getTypecheckingState().record(def.getReferable(), typedDef);
    typedDef.setThisClass(enclosingClass);
    typedDef.setParameters(list.getFirst());
    typedDef.setResultType(expectedType);
    typedDef.setStatus(paramsOk ? Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING : Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
  }

  private static List<Clause> typeCheckFunctionBody(FunctionDefinition typedDef, Concrete.FunctionDefinition def, CheckTypeVisitor visitor) {
    List<Clause> clauses = null;
    Concrete.FunctionBody body = def.getBody();
    Expression expectedType = typedDef.getResultType();

    if (body instanceof Concrete.ElimFunctionBody) {
      if (expectedType != null) {
        Concrete.ElimFunctionBody elimBody = (Concrete.ElimFunctionBody) body;
        List<DependentLink> elimParams = ElimTypechecking.getEliminatedParameters(elimBody.getEliminatedReferences(), elimBody.getClauses(), typedDef.getParameters(), visitor);
        clauses = new ArrayList<>();
        Body typedBody = elimParams == null ? null : new ElimTypechecking(visitor, expectedType, EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CHECK_COVERAGE, PatternTypechecking.Flag.CONTEXT_FREE, PatternTypechecking.Flag.ALLOW_INTERVAL, PatternTypechecking.Flag.ALLOW_CONDITIONS)).typecheckElim(elimBody.getClauses(), def, def.getParameters(), typedDef.getParameters(), elimParams, clauses);
        if (typedBody != null) {
          typedDef.setBody(typedBody);
          typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          if (ConditionsChecking.check(typedBody, clauses, typedDef, def, visitor.getErrorReporter())) {
            typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          } else {
            typedDef.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          }
        } else {
          clauses = null;
        }
      } else {
        if (def.getResultType() == null) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Cannot infer type of the expression", body));
        }
      }
    } else {
      CheckTypeVisitor.Result termResult = visitor.finalCheckExpr(((Concrete.TermFunctionBody) body).getTerm(), expectedType, true);
      if (termResult != null) {
        if (termResult.expression != null) {
          typedDef.setBody(new LeafElimTree(typedDef.getParameters(), termResult.expression));
          clauses = Collections.emptyList();
        }
        typedDef.setResultType(termResult.type);
      }
    }

    typedDef.setStatus(typedDef.getResultType() == null ? Definition.TypeCheckingStatus.HEADER_HAS_ERRORS : typedDef.getBody() == null ? Definition.TypeCheckingStatus.BODY_HAS_ERRORS : Definition.TypeCheckingStatus.NO_ERRORS);
    return clauses;
  }

  private static void typeCheckDataHeader(DataDefinition dataDefinition, Concrete.DataDefinition def, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    LinkList list = initializeThisParam(visitor, enclosingClass);

    Sort userSort = null;
    boolean paramsOk = typeCheckParameters(def.getParameters(), list, visitor, localInstancePool, null) != null;

    if (def.getUniverse() != null) {
      Type userTypeResult = visitor.finalCheckType(def.getUniverse(), ExpectedType.OMEGA);
      if (userTypeResult != null) {
        userSort = userTypeResult.getExpr().toSort();
        if (userSort == null) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Expected a universe", def.getUniverse()));
        }
      }
    }

    dataDefinition.setThisClass(enclosingClass);
    dataDefinition.setParameters(list.getFirst());
    dataDefinition.setSort(userSort);
    visitor.getTypecheckingState().record(def.getReferable(), dataDefinition);

    if (!paramsOk) {
      dataDefinition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          visitor.getTypecheckingState().record(constructor.getReferable(), new Constructor(constructor.getReferable(), dataDefinition));
        }
      }
    } else {
      dataDefinition.setStatus(Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING);
    }
  }

  private static boolean typeCheckDataBody(DataDefinition dataDefinition, Concrete.DataDefinition def, CheckTypeVisitor visitor, boolean polyHLevel, Set<DataDefinition> dataDefinitions) {
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
    if (def.getConstructorClauses().size() > 1 || !def.getConstructorClauses().isEmpty() && def.getConstructorClauses().get(0).getConstructors().size() > 1) {
      inferredSort = inferredSort.max(Sort.SET0);
    }

    boolean dataOk = true;
    List<DependentLink> elimParams = Collections.emptyList();
    if (def.getEliminatedReferences() != null) {
      elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getConstructorClauses(), dataDefinition.getParameters(), visitor);
      if (elimParams == null) {
        dataOk = false;
      }
    }

    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        visitor.getTypecheckingState().record(constructor.getReferable(), new Constructor(constructor.getReferable(), dataDefinition));
      }
    }

    LocalErrorReporter errorReporter = visitor.getErrorReporter();
    LocalErrorReporterCounter countingErrorReporter = new LocalErrorReporterCounter(Error.Level.ERROR, errorReporter);
    visitor.setErrorReporter(countingErrorReporter);

    PatternTypechecking dataPatternTypechecking = new PatternTypechecking(visitor.getErrorReporter(), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE));
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      // Typecheck patterns and compute free bindings
      Pair<List<Pattern>, List<Expression>> result = null;
      if (clause.getPatterns() != null) {
        if (def.getEliminatedReferences() == null) {
          errorReporter.report(new LocalTypeCheckingError("Expected a constructor without patterns", clause));
          dataOk = false;
        }
        if (elimParams != null) {
          result = dataPatternTypechecking.typecheckPatterns(clause.getPatterns(), def.getParameters(), dataDefinition.getParameters(), elimParams, def, visitor);
          if (result != null && result.proj2 == null) {
            errorReporter.report(new LocalTypeCheckingError("This clause is redundant", clause));
            result = null;
          }
        }
        if (result == null) {
          continue;
        }
      } else {
        if (def.getEliminatedReferences() != null) {
          errorReporter.report(new LocalTypeCheckingError("Expected constructors with patterns", clause));
          dataOk = false;
        }
      }

      // Typecheck constructors
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        Patterns patterns = result == null ? null : new Patterns(result.proj1);
        Sort conSort = typeCheckConstructor(constructor, patterns, dataDefinition, visitor, dataDefinitions, userSort);
        if (conSort == null) {
          dataOk = false;
          conSort = Sort.PROP;
        }

        inferredSort = inferredSort.max(conSort);
      }
    }
    dataDefinition.setStatus(dataOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.BODY_HAS_ERRORS);

    visitor.setErrorReporter(errorReporter);

    // Check if constructors pattern match on the interval
    for (Constructor constructor : dataDefinition.getConstructors()) {
      if (constructor.getBody() != null) {
        if (!dataDefinition.matchesOnInterval() && constructor.getBody() instanceof IntervalElim) {
          dataDefinition.setMatchesOnInterval();
          inferredSort = inferredSort.max(new Sort(inferredSort.getPLevel(), Level.INFINITY));
        }
      }
    }

    // Find covariant parameters
    int index = 0;
    for (DependentLink link = dataDefinition.getParameters(); link.hasNext(); link = link.getNext(), index++) {
      boolean isCovariant = true;
      for (Constructor constructor : dataDefinition.getConstructors()) {
        if (!constructor.status().headerIsOK()) {
          continue;
        }
        for (DependentLink link1 = constructor.getParameters(); link1.hasNext(); link1 = link1.getNext()) {
          link1 = link1.getNextTyped(null);
          if (!checkPositiveness(link1.getTypeExpr(), index, null, null, null, Collections.singleton(link))) {
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

    // Check truncatedness
    if (def.isTruncated()) {
      if (userSort == null) {
        String msg = "The data type cannot be truncated since its universe is not specified";
        errorReporter.report(new LocalTypeCheckingError(Error.Level.WARNING, msg, def));
      } else {
        if (inferredSort.isLessOrEquals(userSort)) {
          String msg = "The data type will not be truncated since it already fits in the specified universe";
          errorReporter.report(new LocalTypeCheckingError(Error.Level.WARNING, msg, def.getUniverse()));
        } else {
          dataDefinition.setIsTruncated(true);
        }
      }
    } else if (countingErrorReporter.getErrorsNumber() == 0 && userSort != null && !inferredSort.isLessOrEquals(userSort)) {
      String msg = "Actual universe " + inferredSort + " is not compatible with expected universe " + userSort;
      countingErrorReporter.report(new LocalTypeCheckingError(msg, def.getUniverse()));
    }

    dataDefinition.setSort(countingErrorReporter.getErrorsNumber() == 0 && userSort != null ? userSort : inferredSort);
    return countingErrorReporter.getErrorsNumber() == 0;
  }

  private static Sort typeCheckConstructor(Concrete.Constructor def, Patterns patterns, DataDefinition dataDefinition, CheckTypeVisitor visitor, Set<DataDefinition> dataDefinitions, Sort userSort) {
    Constructor constructor = new Constructor(def.getReferable(), dataDefinition);
    List<DependentLink> elimParams = null;
    Sort sort;

    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(visitor.getFreeBindings())) {
      try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(visitor.getContext())) {
        visitor.getTypecheckingState().record(def.getReferable(), constructor);
        dataDefinition.addConstructor(constructor);

        LinkList list = new LinkList();
        sort = typeCheckParameters(def.getParameters(), list, visitor, null, userSort);

        int index = 0;
        for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext(), index++) {
          link = link.getNextTyped(null);
          if (!checkPositiveness(link.getTypeExpr(), index, def.getParameters(), def, visitor.getErrorReporter(), dataDefinitions)) {
            return null;
          }
        }

        constructor.setParameters(list.getFirst());
        constructor.setPatterns(patterns);
        constructor.setThisClass(dataDefinition.getThisClass());

        if (!def.getClauses().isEmpty()) {
          elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getClauses(), constructor.getParameters(), visitor);
        }
      }
    }

    if (elimParams != null) {
      try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(visitor.getFreeBindings())) {
        try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(visitor.getContext())) {
          List<Clause> clauses = new ArrayList<>();
          Body body = new ElimTypechecking(visitor, constructor.getDataTypeExpression(Sort.STD), EnumSet.of(PatternTypechecking.Flag.ALLOW_INTERVAL, PatternTypechecking.Flag.ALLOW_CONDITIONS)).typecheckElim(def.getClauses(), def, def.getParameters(), constructor.getParameters(), elimParams, clauses);
          constructor.setBody(body);
          constructor.setClauses(clauses);
          constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          ConditionsChecking.check(body, clauses, constructor, def, visitor.getErrorReporter());
        }
      }
    }

    constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    return sort;
  }

  private static boolean checkPositiveness(Expression type, int index, List<? extends Concrete.Parameter> parameters, Concrete.Constructor constructor, LocalErrorReporter errorReporter, Set<? extends Variable> variables) {
    List<SingleDependentLink> piParams = new ArrayList<>();
    type = type.getPiParameters(piParams, false);
    for (DependentLink piParam : piParams) {
      if (piParam instanceof UntypedDependentLink) {
        continue;
      }
      if (!checkNonPositiveError(piParam.getTypeExpr(), index, parameters, constructor, errorReporter, variables)) {
        return false;
      }
    }

    DataCallExpression dataCall = type.checkedCast(DataCallExpression.class);
    if (dataCall != null) {
      List<? extends Expression> exprs = dataCall.getDefCallArguments();
      DataDefinition typeDef = dataCall.getDefinition();

      for (int i = 0; i < exprs.size(); i++) {
        if (typeDef.isCovariant(i)) {
          Expression expr = exprs.get(i).normalize(NormalizeVisitor.Mode.WHNF);
          while (expr.isInstance(LamExpression.class)) {
            expr = expr.cast(LamExpression.class).getBody().normalize(NormalizeVisitor.Mode.WHNF);
          }
          if (!checkPositiveness(expr, index, parameters, constructor, errorReporter, variables)) {
            return false;
          }
        } else {
          if (!checkNonPositiveError(exprs.get(i), index, parameters, constructor, errorReporter, variables)) {
            return false;
          }
        }
      }
    } else if (type.isInstance(AppExpression.class)) {
      for (; type.isInstance(AppExpression.class); type = type.cast(AppExpression.class).getFunction()) {
        if (!checkNonPositiveError(type.cast(AppExpression.class).getArgument(), index, parameters, constructor, errorReporter, variables)) {
          return false;
        }
      }
      if (!type.isInstance(ReferenceExpression.class)) {
        if (!checkNonPositiveError(type, index, parameters, constructor, errorReporter, variables)) {
          return false;
        }
      }
    } else {
      if (!checkNonPositiveError(type, index, parameters, constructor, errorReporter, variables)) {
        return false;
      }
    }

    return true;
  }

  private static boolean checkNonPositiveError(Expression expr, int index, List<? extends Concrete.Parameter> parameters, Concrete.Constructor constructor, LocalErrorReporter errorReporter, Set<? extends Variable> variables) {
    Variable def = expr.findBinding(variables);
    if (def == null) {
      return true;
    }

    if (errorReporter == null) {
      return false;
    }

    int i = 0;
    Concrete.Parameter parameter = null;
    for (Concrete.Parameter parameter1 : parameters) {
      if (parameter1 instanceof Concrete.TelescopeParameter) {
        i += ((Concrete.TelescopeParameter) parameter1).getReferableList().size();
      } else {
        i++;
      }
      if (i > index) {
        parameter = parameter1;
        break;
      }
    }

    errorReporter.report(new NonPositiveDataError((DataDefinition) def, constructor, parameter == null ? constructor : parameter));
    return false;
  }

  private static void typeCheckClass(Concrete.ClassDefinition def, ClassDefinition typedDef, ClassDefinition enclosingClass, CheckTypeVisitor visitor) {
    LocalErrorReporter errorReporter = visitor.getErrorReporter();
    boolean classOk = true;

    typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
    typedDef.setThisClass(enclosingClass);

    if (enclosingClass != null) {
      // TODO[classes]: This looks suspicious
      DependentLink thisParam = createThisParam(enclosingClass);
      visitor.getFreeBindings().add(thisParam);
      visitor.setThis(enclosingClass, thisParam);
    }

    List<GlobalReferable> alreadyImplementFields = new ArrayList<>();
    Concrete.SourceNode alreadyImplementedSourceNode = null;

    for (Concrete.ReferenceExpression aSuperClass : def.getSuperClasses()) {
      CheckTypeVisitor.Result result = visitor.finalCheckExpr(aSuperClass, null, false);
      if (result == null) {
        classOk = false;
        continue;
      }

      ClassCallExpression typeCheckedSuperClass = result.expression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class);
      if (typeCheckedSuperClass == null) {
        errorReporter.report(new LocalTypeCheckingError("Parent must be a class", aSuperClass));
        classOk = false;
        continue;
      }

      typedDef.addFields(typeCheckedSuperClass.getDefinition().getFields());
      typedDef.addSuperClass(typeCheckedSuperClass.getDefinition());

      for (Map.Entry<ClassField, ClassDefinition.Implementation> entry : typeCheckedSuperClass.getDefinition().getImplemented()) {
        if (!implementField(entry.getKey(), entry.getValue(), typedDef, alreadyImplementFields)) {
          classOk = false;
          alreadyImplementedSourceNode = aSuperClass;
        }
      }

      for (Map.Entry<ClassField, Expression> entry : typeCheckedSuperClass.getImplementedHere().entrySet()) {
        if (!implementField(entry.getKey(), new ClassDefinition.Implementation(createThisParam(typedDef), entry.getValue()), typedDef, alreadyImplementFields)) {
          classOk = false;
          alreadyImplementedSourceNode = aSuperClass;
        }
      }
    }

    for (Concrete.ClassField field : def.getFields()) {
      typeCheckClassField(field, typedDef, visitor);
    }

    if (!def.getImplementations().isEmpty()) {
      typedDef.updateSorts();
      for (Concrete.ClassFieldImpl implementation : def.getImplementations()) {
        ClassField field = (ClassField) visitor.getTypecheckingState().getTypechecked((GlobalReferable) implementation.getImplementedField()); // TODO[abstract]: check that it is a field and that it belongs to the class, also check that referable is global
        if (typedDef.isImplemented(field)) {
          classOk = false;
          alreadyImplementFields.add(field.getReferable());
          alreadyImplementedSourceNode = implementation;
          continue;
        }

        TypedDependentLink thisParameter = createThisParam(typedDef);
        visitor.getFreeBindings().add(thisParameter);
        visitor.setThis(typedDef, thisParameter);
        CheckTypeVisitor.Result result = visitor.finalCheckExpr(implementation.getImplementation(), field.getBaseType(Sort.STD).subst(field.getThisParameter(), new ReferenceExpression(thisParameter)), false);
        typedDef.implementField(field, new ClassDefinition.Implementation(thisParameter, result != null ? result.expression : new ErrorExpression(null, null)));
        if (result == null || result.expression.isInstance(ErrorExpression.class)) {
          classOk = false;
        }
      }
    }

    if (!alreadyImplementFields.isEmpty()) {
      errorReporter.report(new FieldsImplementationError(true, alreadyImplementFields, alreadyImplementFields.size() > 1 ? def : alreadyImplementedSourceNode));
    }

    if (classOk) {
      typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    }
    typedDef.updateSorts();
  }

  private static void typeCheckClassField(Concrete.ClassField def, ClassDefinition enclosingClass, CheckTypeVisitor visitor) {
    TypedDependentLink thisParameter = createThisParam(enclosingClass);
    visitor.getFreeBindings().add(thisParameter);
    visitor.setThis(enclosingClass, thisParameter);
    Type typeResult = visitor.finalCheckType(def.getResultType(), ExpectedType.OMEGA);

    ClassField typedDef = new ClassField(def.getReferable(), typeResult == null ? new ErrorExpression(null, null) : typeResult.getExpr(), enclosingClass, thisParameter);
    if (typeResult == null) {
      typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
    }
    visitor.getTypecheckingState().record(def.getReferable(), typedDef);
    enclosingClass.addField(typedDef);
    enclosingClass.addPersonalField(typedDef);
  }

  private static boolean implementField(ClassField classField, ClassDefinition.Implementation implementation, ClassDefinition classDef, List<GlobalReferable> alreadyImplemented) {
    ClassDefinition.Implementation oldImpl = classDef.getImplementation(classField);
    if (oldImpl == null || oldImpl.term.isInstance(ErrorExpression.class)) {
      classDef.implementField(classField, implementation);
    }
    if (oldImpl != null && !oldImpl.substThisParam(new ReferenceExpression(implementation.thisParam)).equals(implementation.term)) {
      alreadyImplemented.add(classField.getReferable());
      return false;
    } else {
      return true;
    }
  }

  private static void typeCheckInstance(Concrete.Instance def, FunctionDefinition typedDef, CheckTypeVisitor visitor) {
    throw new NotImplementedException(); // TODO[abstract]
    /*
    LocalErrorReporter errorReporter = visitor.getErrorReporter();

    LinkList list = new LinkList();
    boolean paramsOk = typeCheckParameters(def.getParameters(), list, visitor, null, null) != null;
    typedDef.setParameters(list.getFirst());
    typedDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
    if (!paramsOk) {
      return;
    }

    Map<ClassField, Concrete.ClassFieldImpl> classFieldMap = new HashMap<>();
    List<GlobalReferable> alreadyImplementedFields = new ArrayList<>();
    Concrete.SourceNode alreadyImplementedSourceNode = null;
    for (Concrete.ClassFieldImpl classFieldImpl : def.getClassFieldImpls()) {
      ClassField field = (ClassField) visitor.getTypecheckingState().getTypechecked((GlobalReferable) classFieldImpl.getImplementedField()); // TODO[abstract]: check that it is a field and that it belongs to the class, also check that referable is global
      if (classFieldMap.containsKey(field)) {
        alreadyImplementedFields.add(field.getReferable());
        alreadyImplementedSourceNode = classFieldImpl;
      } else {
        classFieldMap.put(field, classFieldImpl);
      }
    }
    if (!alreadyImplementedFields.isEmpty()) {
      visitor.getErrorReporter().report(new FieldsImplementationError<>(true, alreadyImplementedFields, alreadyImplementedFields.size() > 1 ? def : alreadyImplementedSourceNode));
    }

    Concrete.ClassView classView = (Concrete.ClassView) def.getClassView().getReferent();
    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassDefinition classDef = (ClassDefinition) visitor.getTypecheckingState().getTypechecked((GlobalReferable) classView.getUnderlyingClass().getReferent()); // TODO[abstract]: check that it is a class, also check that referable is global and there is no left part in underlying class or whatever
    ClassCallExpression term = new ClassCallExpression(classDef, Sort.generateInferVars(visitor.getEquations(), def.getClassView()), fieldSet, Sort.PROP);

    List<GlobalReferable> notImplementedFields = new ArrayList<>();
    for (ClassField field : classDef.getFields()) {
      Concrete.ClassFieldImpl impl = classFieldMap.get(field);
      if (impl != null) {
        if (notImplementedFields.isEmpty()) {
          fieldSet.put(field, visitor.typecheckImplementation(field, impl.getImplementation(), term));
          classFieldMap.remove(field);
        }
      } else {
        notImplementedFields.add(field.getReferable());
      }
    }
    if (!notImplementedFields.isEmpty()) {
      visitor.getErrorReporter().report(new FieldsImplementationError<>(false, notImplementedFields, def));
      return;
    }

    LevelSubstitution substitution = visitor.getEquations().solve(def);
    if (!substitution.isEmpty()) {
      term = new SubstVisitor(new ExprSubstitution(), substitution).visitClassCall(term, null);
    }
    term = new StripVisitor(visitor.getErrorReporter()).visitClassCall(term, null);

    ClassField classifyingField = (ClassField) visitor.getTypecheckingState().getTypechecked((GlobalReferable) classView.getClassifyingField()); // TODO[abstract]: check that it is a field and that it belongs to the class, also check that referable is global
    Expression impl = fieldSet.get(classifyingField);
    DefCallExpression defCall = impl.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DefCallExpression.class);
    if (defCall == null || !defCall.getDefCallArguments().isEmpty()) {
      errorReporter.report(new LocalTypeCheckingError<>("Expected a definition in the classifying field", def));
      return;
    }

    typedDef.setResultType(term);
    typedDef.setBody(new LeafElimTree(list.getFirst(), new NewExpression(term)));
    typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    */
  }
}
