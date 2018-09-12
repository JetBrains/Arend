package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.*;
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
import com.jetbrains.jetpad.vclang.error.IncorrectExpressionException;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporterCounter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.instance.pool.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.instance.pool.LocalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ConditionsChecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ElimTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.PatternTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.parameter;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

public class DefinitionTypechecking implements ConcreteDefinitionVisitor<Boolean, List<Clause>> {
  private CheckTypeVisitor myVisitor;
  private GlobalInstancePool myInstancePool;

  public DefinitionTypechecking(CheckTypeVisitor visitor) {
    myVisitor = visitor;
    myInstancePool = visitor == null ? null : visitor.getInstancePool();
  }

  public void setVisitor(CheckTypeVisitor visitor) {
    myVisitor = visitor;
    myInstancePool = visitor.getInstancePool();
  }

  public Definition typecheckHeader(GlobalInstancePool instancePool, Concrete.Definition definition) {
    LocalInstancePool localInstancePool = new LocalInstancePool(myVisitor);
    instancePool.setInstancePool(localInstancePool);
    myVisitor.setInstancePool(instancePool);
    Definition typechecked = myVisitor.getTypecheckingState().getTypechecked(definition.getData());

    if (definition instanceof Concrete.FunctionDefinition) {
      FunctionDefinition functionDef = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(definition.getData());
      try {
        typecheckFunctionHeader(functionDef, (Concrete.FunctionDefinition) definition, localInstancePool, true);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), definition));
      }
      return functionDef;
    } else
    if (definition instanceof Concrete.DataDefinition) {
      DataDefinition dataDef = typechecked != null ? (DataDefinition) typechecked : new DataDefinition(definition.getData());
      try {
        typecheckDataHeader(dataDef, (Concrete.DataDefinition) definition, localInstancePool);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), definition));
      }
      if (dataDef.getSort() == null || dataDef.getSort().getPLevel().isInfinity()) {
        myVisitor.getErrorReporter().report(new TypecheckingError("Cannot infer the sort of a recursive data type", definition));
        dataDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      }
      return dataDef;
    } else {
      throw new IllegalStateException();
    }
  }

  public List<Clause> typecheckBody(Definition definition, Concrete.Definition def, Set<DataDefinition> dataDefinitions) {
    if (definition instanceof FunctionDefinition) {
      try {
        return typecheckFunctionBody((FunctionDefinition) definition, (Concrete.FunctionDefinition) def);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), def));
      }
    } else
    if (definition instanceof DataDefinition) {
      try {
        if (!typecheckDataBody((DataDefinition) definition, (Concrete.DataDefinition) def, false, dataDefinitions)) {
          definition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
        }
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), def));
      }
    } else {
      throw new IllegalStateException();
    }
    return null;
  }

  private Definition prepare(Concrete.Definition def) {
    if (def.hasErrors()) {
      myVisitor.setHasErrors();
    }
    return myVisitor.getTypecheckingState().getTypechecked(def.getData());
  }

  @Override
  public List<Clause> visitFunction(Concrete.FunctionDefinition def, Boolean recursive) {
    Definition typechecked = prepare(def);
    LocalInstancePool localInstancePool = new LocalInstancePool(myVisitor);
    myInstancePool.setInstancePool(localInstancePool);
    myVisitor.setInstancePool(myInstancePool);

    FunctionDefinition definition = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(def.getData());
    try {
      typecheckFunctionHeader(definition, def, localInstancePool, recursive);
      return recursive && definition.getResultType() == null ? null : typecheckFunctionBody(definition, def);
    } catch (IncorrectExpressionException e) {
      myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), def));
      return null;
    }
  }

  @Override
  public List<Clause> visitData(Concrete.DataDefinition def, Boolean recursive) {
    Definition typechecked = prepare(def);
    LocalInstancePool localInstancePool = new LocalInstancePool(myVisitor);
    myInstancePool.setInstancePool(localInstancePool);
    myVisitor.setInstancePool(myInstancePool);

    DataDefinition definition = typechecked != null ? (DataDefinition) typechecked : new DataDefinition(def.getData());
    try {
      typecheckDataHeader(definition, def, localInstancePool);
      if (definition.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
        typecheckDataBody(definition, def, true, Collections.singleton(definition));
      }
    } catch (IncorrectExpressionException e) {
      myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), def));
      return null;
    }
    return null;
  }

  @Override
  public List<Clause> visitClass(Concrete.ClassDefinition def, Boolean recursive) {
    Definition typechecked = prepare(def);
    ClassDefinition definition = typechecked != null ? (ClassDefinition) typechecked : new ClassDefinition(def.getData());
    if (typechecked == null) {
      myVisitor.getTypecheckingState().record(def.getData(), definition);
    }
    if (recursive) {
      myVisitor.getErrorReporter().report(new TypecheckingError("A class cannot be recursive", def));
      definition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);

      for (Concrete.ClassField field : def.getFields()) {
        ClassField typedDef = new ClassField(field.getData(), definition, new PiExpression(Sort.STD, new TypedSingleDependentLink(false, "this", new ClassCallExpression(definition, Sort.STD)), new ErrorExpression(null, null)));
        typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
        myVisitor.getTypecheckingState().record(field.getData(), typedDef);
        definition.addField(typedDef);
        definition.addPersonalField(typedDef);
      }
    } else {
      try {
        typecheckClass(def, definition);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), def));
      }
    }
    return null;
  }

  @Override
  public List<Clause> visitInstance(Concrete.Instance def, Boolean recursive) {
    LocalInstancePool localInstancePool = new LocalInstancePool(myVisitor);
    myInstancePool.setInstancePool(localInstancePool);
    myVisitor.setInstancePool(myInstancePool);

    Definition typechecked = prepare(def);
    FunctionDefinition definition = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(def.getData());
    if (typechecked == null) {
      myVisitor.getTypecheckingState().record(def.getData(), definition);
    }
    if (recursive) {
      myVisitor.getErrorReporter().report(new TypecheckingError("An instance cannot be recursive", def));
      definition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
    } else {
      try {
        typecheckInstance(def, definition, localInstancePool);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), def));
      }
    }
    return null;
  }

  private Sort typeCheckParameters(List<? extends Concrete.Parameter> parameters, LinkList list, LocalInstancePool localInstancePool, Sort expectedSort) {
    Sort sort = Sort.PROP;
    int index = 0;

    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TypeParameter) {
        Concrete.TypeParameter typeParameter = (Concrete.TypeParameter) parameter;
        Type paramResult = myVisitor.finalCheckType(typeParameter.getType(), expectedSort == null ? ExpectedType.OMEGA : new UniverseExpression(expectedSort));
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
            myVisitor.getContext().put(referableList.get(i), link);
          }
        } else {
          param = parameter(parameter.getExplicit(), (String) null, paramResult);
          index++;
        }

        if (localInstancePool != null) {
          TCClassReferable classRef = typeParameter.getType().getUnderlyingClassReferable(false);
          if (classRef != null) {
            ClassDefinition classDef = (ClassDefinition) myVisitor.getTypecheckingState().getTypechecked(classRef.getUnderlyingTypecheckable());
            if (!classDef.isRecord()) {
              ClassField classifyingField = classDef.getClassifyingField();
              for (DependentLink link = param; link.hasNext(); link = link.getNext()) {
                ReferenceExpression reference = new ReferenceExpression(link);
                // Expression oldInstance =
                  localInstancePool.addInstance(classifyingField == null ? null : FieldCallExpression.make(classifyingField, paramResult.getSortOfType(), reference), classRef, reference, parameter);
                // if (oldInstance != null) {
                //   myVisitor.getErrorReporter().report(new DuplicateInstanceError(oldInstance, reference, parameter));
                // }
              }
            }
          }
        }

        list.append(param);
        for (; param.hasNext(); param = param.getNext()) {
          myVisitor.getFreeBindings().add(param);
        }
      } else {
        myVisitor.getErrorReporter().report(new ArgInferenceError(typeOfFunctionArg(++index), parameter, new Expression[0]));
        sort = null;
      }
    }

    return sort;
  }

  private void typecheckFunctionHeader(FunctionDefinition typedDef, Concrete.FunctionDefinition def, LocalInstancePool localInstancePool, boolean recursive) {
    LinkList list = new LinkList();

    boolean paramsOk = typeCheckParameters(def.getParameters(), list, localInstancePool, null) != null;
    Expression expectedType = null;
    Concrete.Expression resultType = def.getResultType();
    if (resultType != null) {
      Type expectedTypeResult =
        def.getBody() instanceof Concrete.CoelimFunctionBody ? null :
        def.getBody() instanceof Concrete.TermFunctionBody ? myVisitor.checkType(resultType, ExpectedType.OMEGA) : myVisitor.finalCheckType(resultType, ExpectedType.OMEGA);
      if (expectedTypeResult != null) {
        expectedType = expectedTypeResult.getExpr();
      }
    }

    myVisitor.getTypecheckingState().record(def.getData(), typedDef);
    typedDef.setParameters(list.getFirst());
    typedDef.setResultType(expectedType);
    typedDef.setStatus(paramsOk && expectedType != null ? Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING : Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);

    if (recursive && expectedType == null) {
      myVisitor.getErrorReporter().report(new TypecheckingError(def.getBody() instanceof Concrete.CoelimFunctionBody
        ? "Function defined by copattern matching cannot be recursive"
        : "Cannot infer the result type of a recursive function", def));
    }
  }

  private List<Clause> typecheckFunctionBody(FunctionDefinition typedDef, Concrete.FunctionDefinition def) {
    List<Clause> clauses = null;
    Concrete.FunctionBody body = def.getBody();
    Expression expectedType = typedDef.getResultType();
    boolean bodyIsOK = false;

    if (body instanceof Concrete.ElimFunctionBody) {
      if (expectedType != null) {
        Concrete.ElimFunctionBody elimBody = (Concrete.ElimFunctionBody) body;
        List<DependentLink> elimParams = ElimTypechecking.getEliminatedParameters(elimBody.getEliminatedReferences(), elimBody.getClauses(), typedDef.getParameters(), myVisitor);
        clauses = new ArrayList<>();
        Body typedBody = elimParams == null ? null : new ElimTypechecking(myVisitor, expectedType, EnumSet.of(PatternTypechecking.Flag.CHECK_COVERAGE, PatternTypechecking.Flag.CONTEXT_FREE, PatternTypechecking.Flag.ALLOW_INTERVAL, PatternTypechecking.Flag.ALLOW_CONDITIONS)).typecheckElim(elimBody.getClauses(), def, def.getParameters(), typedDef.getParameters(), elimParams, clauses);
        if (typedBody != null) {
          typedDef.setBody(typedBody);
          typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          if (ConditionsChecking.check(typedBody, clauses, typedDef, def, myVisitor.getErrorReporter())) {
            typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          } else {
            typedDef.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          }
        } else {
          clauses = null;
        }
      } else {
        if (def.getResultType() == null) {
          myVisitor.getErrorReporter().report(new TypecheckingError("Cannot infer type of a function defined by pattern matching", def));
        }
      }
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      if (def.getResultType() == null) {
        myVisitor.getErrorReporter().report(new TypecheckingError("Cannot infer type of a function defined by copattern matching", def));
      } else {
        typecheckCoClauses(typedDef, def, def.getResultType(), body.getClassFieldImpls());
        bodyIsOK = true;
      }
    } else {
      CheckTypeVisitor.Result termResult = myVisitor.finalCheckExpr(((Concrete.TermFunctionBody) body).getTerm(), expectedType, true);
      if (termResult != null) {
        if (termResult.expression != null) {
          typedDef.setBody(new LeafElimTree(typedDef.getParameters(), termResult.expression));
          clauses = Collections.emptyList();
        }
        if (termResult.expression instanceof NewExpression) {
          bodyIsOK = true;
          typedDef.setBody(null);
          typedDef.setResultType(((NewExpression) termResult.expression).getExpression());
        } else {
          typedDef.setResultType(termResult.type);
        }
      }
    }

    typedDef.setStatus(typedDef.getResultType() == null ? Definition.TypeCheckingStatus.HEADER_HAS_ERRORS : !bodyIsOK && typedDef.getBody() == null ? Definition.TypeCheckingStatus.BODY_HAS_ERRORS : myVisitor.getStatus());

    if (def.isCoerce() && typedDef.getBody() != null) {
      Definition coerceParent = myVisitor.getTypecheckingState().getTypechecked(def.getCoerceParent());
      if (coerceParent instanceof DataDefinition || coerceParent instanceof ClassDefinition) {
        if (def.getParameters().isEmpty()) {
          myVisitor.getErrorReporter().report(new TypecheckingError("\\coerce must have at least one parameter", def));
        } else {
          Definition paramDef = null;
          Concrete.Parameter parameter = def.getParameters().get(def.getParameters().size() - 1);
          if (parameter instanceof Concrete.TypeParameter) {
            paramDef = getExpressionDef(((Concrete.TypeParameter) parameter).getType());
          }

          DefCallExpression resultDefCall = typedDef.getResultType().checkedCast(DefCallExpression.class);
          Definition resultDef = resultDefCall == null ? null : resultDefCall.getDefinition();

          if ((resultDef == coerceParent) == (paramDef == coerceParent)) {
            myVisitor.getErrorReporter().report(new TypecheckingError("Either the last parameter or the result type (but not both) of \\coerce must be the parent definition", def));
          } else {
            if (resultDef == coerceParent) {
              coerceParent.getCoerceData().addCoerceFrom(paramDef, typedDef);
            } else {
              coerceParent.getCoerceData().addCoerceTo(resultDef, typedDef);
            }
          }
        }
      } else {
        myVisitor.getErrorReporter().report(new TypecheckingError("\\coerce is allowed only in \\where block of \\data and \\class", def));
      }
    }

    return clauses;
  }

  private Definition getExpressionDef(Concrete.Expression expr) {
    Referable ref = expr.getUnderlyingReferable();
    return ref instanceof TCReferable ? myVisitor.getTypecheckingState().getTypechecked((TCReferable) ref) : null;
  }

  private void typecheckDataHeader(DataDefinition dataDefinition, Concrete.DataDefinition def, LocalInstancePool localInstancePool) {
    LinkList list = new LinkList();

    Sort userSort = null;
    boolean paramsOk = typeCheckParameters(def.getParameters(), list, localInstancePool, null) != null;

    if (def.getUniverse() != null) {
      Type userTypeResult = myVisitor.finalCheckType(def.getUniverse(), ExpectedType.OMEGA);
      if (userTypeResult != null) {
        userSort = userTypeResult.getExpr().toSort();
        if (userSort == null) {
          myVisitor.getErrorReporter().report(new TypecheckingError("Expected a universe", def.getUniverse()));
        }
      }
    }

    dataDefinition.setParameters(list.getFirst());
    dataDefinition.setSort(userSort);
    myVisitor.getTypecheckingState().record(def.getData(), dataDefinition);

    if (!paramsOk) {
      dataDefinition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          myVisitor.getTypecheckingState().record(constructor.getData(), new Constructor(constructor.getData(), dataDefinition));
        }
      }
    } else {
      dataDefinition.setStatus(Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING);
    }
  }

  private boolean typecheckDataBody(DataDefinition dataDefinition, Concrete.DataDefinition def, boolean polyHLevel, Set<DataDefinition> dataDefinitions) {
    dataDefinition.getConstructors().clear();

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
      elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getConstructorClauses(), dataDefinition.getParameters(), myVisitor);
      if (elimParams == null) {
        dataOk = false;
      }
    }

    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        myVisitor.getTypecheckingState().record(constructor.getData(), new Constructor(constructor.getData(), dataDefinition));
      }
    }

    LocalErrorReporter errorReporter = myVisitor.getErrorReporter();
    LocalErrorReporterCounter countingErrorReporter = new LocalErrorReporterCounter(Error.Level.ERROR, errorReporter);
    myVisitor.setErrorReporter(countingErrorReporter);

    if (!def.getConstructorClauses().isEmpty()) {
      Map<Referable, Binding> context = myVisitor.getContext();
      Set<Binding> freeBindings = myVisitor.getFreeBindings();
      PatternTypechecking dataPatternTypechecking = new PatternTypechecking(myVisitor.getErrorReporter(), EnumSet.of(PatternTypechecking.Flag.CONTEXT_FREE));

      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        myVisitor.setContext(new HashMap<>(context));
        myVisitor.setFreeBindings(new HashSet<>(freeBindings));

        // Typecheck patterns and compute free bindings
        Pair<List<Pattern>, List<Expression>> result = null;
        if (clause.getPatterns() != null) {
          if (def.getEliminatedReferences() == null) {
            errorReporter.report(new TypecheckingError("Expected a constructor without patterns", clause));
            dataOk = false;
          }
          if (elimParams != null) {
            result = dataPatternTypechecking.typecheckPatterns(clause.getPatterns(), def.getParameters(), dataDefinition.getParameters(), elimParams, def, myVisitor);
            if (result != null && result.proj2 == null) {
              errorReporter.report(new TypecheckingError("This clause is redundant", clause));
              result = null;
            }
            if (result == null) {
              myVisitor.setContext(new HashMap<>(context));
              myVisitor.setFreeBindings(new HashSet<>(freeBindings));
              fillInPatterns(clause.getPatterns());
            }
          }
        } else {
          if (def.getEliminatedReferences() != null) {
            errorReporter.report(new TypecheckingError("Expected constructors with patterns", clause));
            dataOk = false;
          }
        }

        // Typecheck constructors
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          Patterns patterns = result == null ? null : new Patterns(result.proj1);
          Sort conSort = typecheckConstructor(constructor, patterns, dataDefinition, dataDefinitions, userSort);
          if (conSort == null) {
            dataOk = false;
            conSort = Sort.PROP;
          }

          inferredSort = inferredSort.max(conSort);
        }
      }
    }
    dataDefinition.setStatus(dataOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.BODY_HAS_ERRORS);

    myVisitor.setErrorReporter(errorReporter);

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
        errorReporter.report(new TypecheckingError(Error.Level.WARNING, msg, def));
      } else {
        if (inferredSort.isLessOrEquals(userSort)) {
          String msg = "The data type will not be truncated since it already fits in the specified universe";
          errorReporter.report(new TypecheckingError(Error.Level.WARNING, msg, def.getUniverse()));
        } else {
          dataDefinition.setIsTruncated(true);
        }
      }
    } else if (countingErrorReporter.getErrorsNumber() == 0 && userSort != null && !inferredSort.isLessOrEquals(userSort)) {
      String msg = "Actual universe " + inferredSort + " is not compatible with expected universe " + userSort;
      countingErrorReporter.report(new TypecheckingError(msg, def.getUniverse()));
    }

    dataDefinition.setSort(countingErrorReporter.getErrorsNumber() == 0 && userSort != null ? userSort : inferredSort);
    if (dataDefinition.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
      dataDefinition.setStatus(myVisitor.getStatus());
    }
    return countingErrorReporter.getErrorsNumber() == 0;
  }

  private void fillInPatterns(List<Concrete.Pattern> patterns) {
    for (Concrete.Pattern pattern : patterns) {
      if (pattern instanceof Concrete.NamePattern) {
        Referable referable = ((Concrete.NamePattern) pattern).getReferable();
        if (referable != null) {
          myVisitor.getContext().put(referable, new TypedBinding(referable.textRepresentation(), new ErrorExpression(null, null)));
        }
      } else if (pattern instanceof Concrete.ConstructorPattern) {
        fillInPatterns(((Concrete.ConstructorPattern) pattern).getPatterns());
      } else if (pattern instanceof Concrete.TuplePattern) {
        fillInPatterns(((Concrete.TuplePattern) pattern).getPatterns());
      }
    }
  }

  private Sort typecheckConstructor(Concrete.Constructor def, Patterns patterns, DataDefinition dataDefinition, Set<DataDefinition> dataDefinitions, Sort userSort) {
    Constructor constructor = new Constructor(def.getData(), dataDefinition);
    constructor.setPatterns(patterns);
    List<DependentLink> elimParams = null;
    Sort sort;

    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myVisitor.getFreeBindings())) {
      try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myVisitor.getContext())) {
        myVisitor.getTypecheckingState().record(def.getData(), constructor);
        dataDefinition.addConstructor(constructor);

        LinkList list = new LinkList();
        sort = typeCheckParameters(def.getParameters(), list, null, userSort);

        int index = 0;
        for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext(), index++) {
          link = link.getNextTyped(null);
          if (!checkPositiveness(link.getTypeExpr(), index, def.getParameters(), def, myVisitor.getErrorReporter(), dataDefinitions)) {
            constructor.setParameters(EmptyDependentLink.getInstance());
            return null;
          }
        }

        constructor.setParameters(list.getFirst());

        if (!def.getClauses().isEmpty()) {
          elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getClauses(), constructor.getParameters(), myVisitor);
        }
      }
    }

    if (elimParams != null) {
      try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myVisitor.getFreeBindings())) {
        try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myVisitor.getContext())) {
          List<Clause> clauses = new ArrayList<>();
          Body body = new ElimTypechecking(myVisitor, constructor.getDataTypeExpression(Sort.STD), EnumSet.of(PatternTypechecking.Flag.ALLOW_INTERVAL, PatternTypechecking.Flag.ALLOW_CONDITIONS)).typecheckElim(def.getClauses(), def, def.getParameters(), constructor.getParameters(), elimParams, clauses);
          constructor.setBody(body);
          constructor.setClauses(clauses);
          constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          ConditionsChecking.check(body, clauses, constructor, def, myVisitor.getErrorReporter());
        }
      }
    }

    constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    return sort;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
        //noinspection RedundantIfStatement
        if (!checkNonPositiveError(type, index, parameters, constructor, errorReporter, variables)) {
          return false;
        }
      }
    } else {
      //noinspection RedundantIfStatement
      if (!checkNonPositiveError(type, index, parameters, constructor, errorReporter, variables)) {
        return false;
      }
    }

    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

  private void typecheckClass(Concrete.ClassDefinition def, ClassDefinition typedDef) {
    typedDef.clear();

    LocalErrorReporter errorReporter = myVisitor.getErrorReporter();
    boolean classOk = true;

    typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<GlobalReferable> alreadyImplementFields = new ArrayList<>();
    Concrete.SourceNode alreadyImplementedSourceNode = null;

    // Process super classes
    for (Concrete.ReferenceExpression aSuperClass : def.getSuperClasses()) {
      ClassDefinition superClass = myVisitor.referableToDefinition(aSuperClass.getReferent(), ClassDefinition.class, "Expected a class", aSuperClass);
      if (superClass == null) {
        continue;
      }

      typedDef.addFields(superClass.getFields());
      typedDef.addSuperClass(superClass);

      for (Map.Entry<ClassField, LamExpression> entry : superClass.getImplemented()) {
        if (!implementField(entry.getKey(), entry.getValue(), typedDef, alreadyImplementFields)) {
          classOk = false;
          alreadyImplementedSourceNode = aSuperClass;
        }
      }
    }

    // Process fields
    Concrete.Expression previousType = null;
    ClassField previousField = null;
    for (int i = 0; i < def.getFields().size(); i++) {
      Concrete.ClassField field = def.getFields().get(i);
      if (previousType == field.getResultType()) {
        addField(field.getData(), typedDef, previousField.getType(Sort.STD), myVisitor.getTypecheckingState()).setStatus(previousField.status());
      } else {
        previousField = typecheckClassField(field, typedDef);
        previousType = field.getResultType();
      }
    }

    // Process coercing field
    if (!def.isRecord()) {
      ClassField classifyingField = null;
      for (ClassDefinition superClass : typedDef.getSuperClasses()) {
        classifyingField = superClass.getClassifyingField();
        if (classifyingField != null) {
          break;
        }
      }
      if (classifyingField == null && def.getCoercingField() != null) {
        Definition definition = myVisitor.getTypecheckingState().getTypechecked(def.getCoercingField());
        if (definition instanceof ClassField && ((ClassField) definition).getParentClass().equals(typedDef)) {
          classifyingField = (ClassField) definition;
          classifyingField.setType(classifyingField.getType(Sort.STD).normalize(NormalizeVisitor.Mode.WHNF));
        } else {
          errorReporter.report(new TypecheckingError("Internal error: coercing field must be a field belonging to the class", def));
        }
      }
      typedDef.setClassifyingField(classifyingField);
      if (classifyingField != null) {
        typedDef.getCoerceData().addCoercingField(classifyingField);
      }
    } else {
      typedDef.setRecord();
    }

    // Process implementations
    if (!def.getImplementations().isEmpty()) {
      typedDef.updateSorts();
      for (Concrete.ClassFieldImpl classFieldImpl : def.getImplementations()) {
        ClassField field = myVisitor.referableToClassField(classFieldImpl.getImplementedField(), classFieldImpl);
        if (field == null) {
          classOk = false;
          continue;
        }
        if (typedDef.isImplemented(field)) {
          classOk = false;
          alreadyImplementFields.add(field.getReferable());
          alreadyImplementedSourceNode = classFieldImpl;
          continue;
        }

        SingleDependentLink parameter = new TypedSingleDependentLink(false, "this", new ClassCallExpression(typedDef, Sort.STD));
        Concrete.LamExpression lamImpl = (Concrete.LamExpression) classFieldImpl.implementation;
        CheckTypeVisitor.Result result;
        if (lamImpl != null) {
          Concrete.TelescopeParameter concreteParameter = (Concrete.TelescopeParameter) lamImpl.getParameters().get(0);
          myVisitor.getContext().put(concreteParameter.getReferableList().get(0), parameter);
          myVisitor.getFreeBindings().add(parameter);
          PiExpression fieldType = field.getType(Sort.STD);
          result = myVisitor.finalCheckExpr(lamImpl.body, fieldType.getCodomain().subst(fieldType.getParameters(), new ReferenceExpression(parameter)), false);
        } else {
          result = null;
        }
        if (result == null || result.expression.isInstance(ErrorExpression.class)) {
          classOk = false;
        }

        if (result != null) {
          Set<ClassField> futureFields = new HashSet<>();
          boolean found = false;
          for (ClassField classField : typedDef.getFields()) {
            if (!found && classField == field) {
              found = true;
            }
            if (found && !typedDef.isImplemented(classField)) {
              futureFields.add(classField);
            }
          }
          Variable var = result.expression.findBinding(futureFields);
          if (var != null) {
            errorReporter.report(new ImplementationReferenceError((ClassField) var, classFieldImpl));
            result = null;
          }
        }

        typedDef.implementField(field, new LamExpression(Sort.STD, parameter, result == null ? new ErrorExpression(null, null) : result.expression));
        myVisitor.getContext().clear();
        myVisitor.getFreeBindings().clear();
      }
    }

    if (!alreadyImplementFields.isEmpty()) {
      errorReporter.report(new FieldsImplementationError(true, alreadyImplementFields, alreadyImplementFields.size() > 1 ? def : alreadyImplementedSourceNode));
    }

    typedDef.setStatus(!classOk ? Definition.TypeCheckingStatus.BODY_HAS_ERRORS : myVisitor.getStatus());
    typedDef.updateSorts();
  }

  private static PiExpression checkFieldType(Type type, ClassDefinition parentClass) {
    if (!(type instanceof PiExpression)) {
      return null;
    }
    PiExpression piExpr = (PiExpression) type;
    if (piExpr.getParameters().getNext().hasNext()) {
      return null;
    }

    Expression parameterType = piExpr.getParameters().getTypeExpr();
    return parameterType instanceof ClassCallExpression && ((ClassCallExpression) parameterType).getDefinition() == parentClass ? (PiExpression) type : null;
  }

  private ClassField typecheckClassField(Concrete.ClassField def, ClassDefinition parentClass) {
    Type typeResult = myVisitor.finalCheckType(def.getResultType(), ExpectedType.OMEGA);
    PiExpression piType = checkFieldType(typeResult, parentClass);
    if (piType == null) {
      TypedSingleDependentLink param = new TypedSingleDependentLink(false, "this", new ClassCallExpression(parentClass, Sort.STD));
      if (typeResult == null) {
        piType = new PiExpression(Sort.STD, param, new ErrorExpression(null, null));
      } else {
        myVisitor.getErrorReporter().report(new TypecheckingError("Internal error: class field must have a function type", def));
        piType = new PiExpression(typeResult.getSortOfType(), param, typeResult.getExpr());
        typeResult = null;
      }
    }

    ClassField typedDef = addField(def.getData(), parentClass, piType, myVisitor.getTypecheckingState());
    if (typeResult == null) {
      typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
    }
    return typedDef;
  }

  private static ClassField addField(TCFieldReferable fieldRef, ClassDefinition parentClass, PiExpression piType, TypecheckerState state) {
    ClassField typedDef = new ClassField(fieldRef, parentClass, piType);
    state.record(fieldRef, typedDef);
    parentClass.addField(typedDef);
    parentClass.addPersonalField(typedDef);
    return typedDef;
  }

  private static boolean implementField(ClassField classField, LamExpression implementation, ClassDefinition classDef, List<GlobalReferable> alreadyImplemented) {
    LamExpression oldImpl = classDef.implementField(classField, implementation);
    if (oldImpl != null && !oldImpl.substArgument(new ReferenceExpression(implementation.getParameters())).equals(implementation.getBody())) {
      alreadyImplemented.add(classField.getReferable());
      return false;
    } else {
      return true;
    }
  }

  private ClassCallExpression typecheckCoClauses(FunctionDefinition typedDef, Concrete.Definition def, Concrete.Expression resultType, List<Concrete.ClassFieldImpl> classFieldImpls) {
    if (resultType instanceof Concrete.ClassExtExpression) {
      ((Concrete.ClassExtExpression) resultType).getStatements().addAll(classFieldImpls);
    } else {
      resultType = new Concrete.ClassExtExpression(def.getData(), resultType, classFieldImpls);
    }
    CheckTypeVisitor.Result result = myVisitor.finalCheckExpr(resultType, ExpectedType.OMEGA, false);
    if (result == null || !(result.expression instanceof ClassCallExpression)) {
      return null;
    }

    ClassCallExpression typecheckedResultType = (ClassCallExpression) result.expression;
    myVisitor.checkAllImplemented(typecheckedResultType, def);
    typedDef.setResultType(typecheckedResultType);
    typedDef.setStatus(myVisitor.getStatus());
    return typecheckedResultType;
  }

  private void typecheckInstance(Concrete.Instance def, FunctionDefinition typedDef, LocalInstancePool localInstancePool) {
    LinkList list = new LinkList();
    boolean paramsOk = typeCheckParameters(def.getParameters(), list, localInstancePool, null) != null;
    typedDef.setParameters(list.getFirst());
    typedDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
    if (!paramsOk) {
      return;
    }

    ClassCallExpression typecheckedResultType = typecheckCoClauses(typedDef, def, def.getResultType(), def.getClassFieldImpls());
    if (typecheckedResultType == null) {
      return;
    }

    ClassField classifyingField = typecheckedResultType.getDefinition().getClassifyingField();
    if (classifyingField != null) {
      Expression classifyingExpr = typecheckedResultType.getImplementationHere(classifyingField);
      if (classifyingExpr != null && !(classifyingExpr instanceof ErrorExpression)) {
        if (!(classifyingExpr instanceof DefCallExpression || classifyingExpr instanceof UniverseExpression || classifyingExpr instanceof IntegerExpression)) {
          myVisitor.getErrorReporter().report(new TypecheckingError(Error.Level.ERROR, "Classifying field must be a defCall or a universe", def.getResultType()));
        }
      }
    }
  }
}
