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
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.StripVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
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
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ConditionsChecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ElimTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.PatternTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.CompositeInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.LocalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.FieldCall;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.parameter;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

class DefinitionTypechecking {
  static Definition typecheckHeader(CheckTypeVisitor visitor, GlobalInstancePool instancePool, Abstract.Definition definition, Abstract.ClassDefinition enclosingClass) {
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

  static List<Clause> typecheckBody(Definition definition, CheckTypeVisitor exprVisitor, Set<DataDefinition> dataDefinitions) {
    if (definition instanceof FunctionDefinition) {
      return typeCheckFunctionBody((FunctionDefinition) definition, exprVisitor);
    } else
    if (definition instanceof DataDefinition) {
      if (!typeCheckDataBody((DataDefinition) definition, exprVisitor, false, dataDefinitions)) {
        definition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      }
    } else {
      throw new IllegalStateException();
    }
    return null;
  }

  static List<Clause> typecheck(TypecheckerState state, GlobalInstancePool instancePool, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, TypecheckingUnit unit, boolean recursive, LocalErrorReporter errorReporter) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(state, staticNsProvider, dynamicNsProvider, new LinkedHashMap<>(), errorReporter, instancePool);
    ClassDefinition enclosingClass = unit.getEnclosingClass() == null ? null : (ClassDefinition) state.getTypechecked(unit.getEnclosingClass());
    Definition typechecked = state.getTypechecked(unit.getDefinition());

    if (unit.getDefinition() instanceof Abstract.ClassDefinition) {
      ClassDefinition definition = typechecked != null ? (ClassDefinition) typechecked : new ClassDefinition((Abstract.ClassDefinition) unit.getDefinition());
      typeCheckClass(definition, enclosingClass, visitor);
      return null;
    } else
    if (unit.getDefinition() instanceof Abstract.ClassViewInstance) {
      FunctionDefinition definition = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(unit.getDefinition());
      typeCheckClassViewInstance(definition, visitor);
      return null;
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
          return null;
        }
      }
      return typeCheckFunctionBody(definition, visitor);
    } else
    if (unit.getDefinition() instanceof Abstract.DataDefinition) {
      DataDefinition definition = typechecked != null ? (DataDefinition) typechecked : new DataDefinition((Abstract.DataDefinition) unit.getDefinition());
      typeCheckDataHeader(definition, enclosingClass, visitor, localInstancePool);
      if (definition.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
        typeCheckDataBody(definition, visitor, true, Collections.singleton(definition));
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

  private static Sort typeCheckParameters(List<? extends Abstract.Argument> arguments, LinkList list, CheckTypeVisitor visitor, LocalInstancePool localInstancePool, Map<Integer, ClassField> classifyingFields) {
    Sort sort = Sort.PROP;
    int index = 0;

    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) argument;
        Type paramResult = visitor.finalCheckType(typeArgument.getType());
        if (paramResult == null) {
          sort = null;
          paramResult = new TypeExpression(new ErrorExpression(null, null), Sort.SET0);
        } else if (sort != null) {
          sort = sort.max(paramResult.getSortOfType());
        }

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          List<? extends Abstract.ReferableSourceNode> referableList = ((Abstract.TelescopeArgument) argument).getReferableList();
          List<String> names = new ArrayList<>(referableList.size());
          for (Abstract.ReferableSourceNode referable : referableList) {
            names.add(referable == null ? null : referable.getName());
          }
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
        for (; param.hasNext(); param = param.getNext()) {
          visitor.getFreeBindings().add(param);
        }
      } else {
        visitor.getErrorReporter().report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0]));
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

  private static void typeCheckFunctionHeader(FunctionDefinition typedDef, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    LinkList list = initializeThisParam(visitor, enclosingClass);

    Map<Integer, ClassField> classifyingFields = new HashMap<>();
    Abstract.FunctionDefinition def = (Abstract.FunctionDefinition) typedDef.getAbstractDefinition();
    boolean paramsOk = typeCheckParameters(def.getArguments(), list, visitor, localInstancePool, classifyingFields) != null;
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

  private static List<Clause> typeCheckFunctionBody(FunctionDefinition typedDef, CheckTypeVisitor visitor) {
    List<Clause> clauses = null;
    Abstract.FunctionDefinition def = (Abstract.FunctionDefinition) typedDef.getAbstractDefinition();
    Abstract.FunctionBody body = def.getBody();

    if (body != null) {
      Expression expectedType = typedDef.getResultType();

      if (body instanceof Abstract.ElimFunctionBody) {
        if (expectedType != null) {
          Abstract.ElimFunctionBody elimBody = (Abstract.ElimFunctionBody) body;
          List<DependentLink> elimParams = ElimTypechecking.getEliminatedParameters(elimBody.getEliminatedReferences(), elimBody.getClauses(), typedDef.getParameters(), visitor);
          clauses = new ArrayList<>();
          Body typedBody = elimParams == null ? null : new ElimTypechecking(visitor, expectedType, EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CHECK_COVERAGE, PatternTypechecking.Flag.CONTEXT_FREE, PatternTypechecking.Flag.ALLOW_INTERVAL, PatternTypechecking.Flag.ALLOW_CONDITIONS)).typecheckElim(elimBody.getClauses(), def, def.getArguments(), typedDef.getParameters(), elimParams, clauses);
          if (typedBody != null) {
            typedDef.setBody(typedBody);
            typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
            if (ConditionsChecking.check(typedBody, clauses, typedDef, visitor.getErrorReporter())) {
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
        CheckTypeVisitor.Result termResult = visitor.finalCheckExpr(((Abstract.TermFunctionBody) body).getTerm(), expectedType);
        if (termResult != null) {
          typedDef.setBody(new LeafElimTree(typedDef.getParameters(), termResult.expression));
          if (expectedType == null) {
            typedDef.setResultType(termResult.type);
          }
          clauses = Collections.emptyList();
        }
      }
    }

    typedDef.setStatus(typedDef.getResultType() == null ? Definition.TypeCheckingStatus.HEADER_HAS_ERRORS : typedDef.getBody() == null ? Definition.TypeCheckingStatus.BODY_HAS_ERRORS : Definition.TypeCheckingStatus.NO_ERRORS);
    return clauses;
  }

  private static void typeCheckDataHeader(DataDefinition dataDefinition, ClassDefinition enclosingClass, CheckTypeVisitor visitor, LocalInstancePool localInstancePool) {
    LinkList list = initializeThisParam(visitor, enclosingClass);

    Map<Integer, ClassField> classifyingFields = new HashMap<>();
    Sort userSort = null;
    Abstract.DataDefinition def = dataDefinition.getAbstractDefinition();
    boolean paramsOk = typeCheckParameters(def.getParameters(), list, visitor, localInstancePool, classifyingFields) != null;

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
      for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
        for (Abstract.Constructor constructor : clause.getConstructors()) {
          visitor.getTypecheckingState().record(constructor, new Constructor(constructor, dataDefinition));
        }
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

    for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
      for (Abstract.Constructor constructor : clause.getConstructors()) {
        visitor.getTypecheckingState().record(constructor, new Constructor(constructor, dataDefinition));
      }
    }

    boolean universeOk = true;
    PatternTypechecking dataPatternTypechecking = new PatternTypechecking(visitor.getErrorReporter(), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE));
    for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
      // Typecheck patterns and compute free bindings
      Pair<List<Pattern>, List<Expression>> result = null;
      if (clause.getPatterns() != null) {
        if (def.getEliminatedReferences() == null) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Expected a constructor without patterns", clause));
          dataOk = false;
        }
        if (elimParams != null) {
          result = dataPatternTypechecking.typecheckPatterns(clause.getPatterns(), def.getParameters(), dataDefinition.getParameters(), elimParams, def, visitor);
          if (result != null && result.proj2 == null) {
            visitor.getErrorReporter().report(new LocalTypeCheckingError("This clause is redundant", clause));
            result = null;
          }
        }
        if (result == null) {
          continue;
        }
      } else {
        if (def.getEliminatedReferences() != null) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Expected constructors with patterns", clause));
          dataOk = false;
        }
      }

      // Typecheck constructors
      for (Abstract.Constructor constructor : clause.getConstructors()) {
        Patterns patterns = result == null ? null : new Patterns(result.proj1);
        Sort conSort = typeCheckConstructor(constructor, patterns, dataDefinition, visitor, dataDefinitions);
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

  private static Sort typeCheckConstructor(Abstract.Constructor def, Patterns patterns, DataDefinition dataDefinition, CheckTypeVisitor visitor, Set<DataDefinition> dataDefinitions) {
    Constructor constructor = new Constructor(def, dataDefinition);
    List<DependentLink> elimParams = null;
    Sort sort;

    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(visitor.getFreeBindings())) {
      try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(visitor.getContext())) {
        visitor.getTypecheckingState().record(def, constructor);
        dataDefinition.addConstructor(constructor);

        LinkList list = new LinkList();
        sort = typeCheckParameters(def.getArguments(), list, visitor, null, null);

        int index = 0;
        for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext(), index++) {
          link = link.getNextTyped(null);
          if (!checkPositiveness(link.getTypeExpr(), index, def.getArguments(), def, visitor.getErrorReporter(), dataDefinitions)) {
            return null;
          }
        }

        constructor.setParameters(list.getFirst());
        constructor.setPatterns(patterns);
        constructor.setThisClass(dataDefinition.getThisClass());

        if (def.getClauses() != null) {
          elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getClauses(), constructor.getParameters(), visitor);
        }
      }
    }

    if (elimParams != null) {
      try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(visitor.getFreeBindings())) {
        try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(visitor.getContext())) {
          List<Clause> clauses = new ArrayList<>();
          Body body = new ElimTypechecking(visitor, constructor.getDataTypeExpression(Sort.STD), EnumSet.of(PatternTypechecking.Flag.ALLOW_INTERVAL, PatternTypechecking.Flag.ALLOW_CONDITIONS)).typecheckElim(def.getClauses(), def, def.getArguments(), constructor.getParameters(), elimParams, clauses);
          constructor.setBody(body);
          constructor.setClauses(clauses);
          constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          ConditionsChecking.check(body, clauses, constructor, visitor.getErrorReporter());
        }
      }
    }

    constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    return sort;
  }

  private static boolean checkPositiveness(Expression type, int index, List<? extends Abstract.Argument> arguments, Abstract.Constructor constructor, LocalErrorReporter errorReporter, Set<? extends Variable> variables) {
    List<SingleDependentLink> piParams = new ArrayList<>();
    type = type.getPiParameters(piParams, false);
    for (DependentLink piParam : piParams) {
      if (piParam instanceof UntypedDependentLink) {
        continue;
      }
      if (!checkNonPositiveError(piParam.getTypeExpr(), index, arguments, constructor, errorReporter, variables)) {
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
          if (!checkPositiveness(expr, index, arguments, constructor, errorReporter, variables)) {
            return false;
          }
        } else {
          if (!checkNonPositiveError(exprs.get(i), index, arguments, constructor, errorReporter, variables)) {
            return false;
          }
        }
      }
    } else if (type.isInstance(AppExpression.class)) {
      for (; type.isInstance(AppExpression.class); type = type.cast(AppExpression.class).getFunction()) {
        if (!checkNonPositiveError(type.cast(AppExpression.class).getArgument(), index, arguments, constructor, errorReporter, variables)) {
          return false;
        }
      }
      if (!type.isInstance(ReferenceExpression.class)) {
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

    String msg = "Non-positive recursive occurrence of data type " + def.getName() + " in constructor " + constructor.getName();
    errorReporter.report(new LocalTypeCheckingError(msg, argument == null ? constructor : argument));
    return false;
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

        ClassCallExpression typeCheckedSuperClass = result.expression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class);
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
          if (result == null || result.expression.isInstance(ErrorExpression.class)) {
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

  @SuppressWarnings("UnusedReturnValue")
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
    boolean paramsOk = typeCheckParameters(def.getArguments(), list, visitor, null, null) != null;
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
    DefCallExpression defCall = impl.term.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DefCallExpression.class);
    if (defCall == null || !defCall.getDefCallArguments().isEmpty()) {
      errorReporter.report(new LocalTypeCheckingError("Expected a definition in the classifying field", def));
      return;
    }

    typedDef.setResultType(term);
    typedDef.setBody(new LeafElimTree(list.getFirst(), new NewExpression(term)));
    typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
  }
}
