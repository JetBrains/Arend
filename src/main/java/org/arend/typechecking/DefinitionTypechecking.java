package org.arend.typechecking;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.*;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.FieldsCollector;
import org.arend.core.expr.visitor.FreeVariablesCollector;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.Pattern;
import org.arend.core.pattern.Patterns;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.error.Error;
import org.arend.error.IncorrectExpressionException;
import org.arend.naming.error.DuplicateNameError;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.term.ClassFieldKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.concrete.FreeReferablesVisitor;
import org.arend.typechecking.error.CycleError;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.LocalErrorReporterCounter;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.instance.pool.LocalInstancePool;
import org.arend.typechecking.patternmatching.ConditionsChecking;
import org.arend.typechecking.patternmatching.ElimTypechecking;
import org.arend.typechecking.patternmatching.PatternTypechecking;
import org.arend.typechecking.visitor.CheckForUniversesVisitor;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.FreeVariablesClassifier;
import org.arend.typechecking.visitor.ReferablesCollector;
import org.arend.util.Pair;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

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

  public Definition typecheckHeader(Definition typechecked, GlobalInstancePool instancePool, Concrete.Definition definition) {
    LocalInstancePool localInstancePool = new LocalInstancePool(myVisitor);
    instancePool.setInstancePool(localInstancePool);
    myVisitor.setInstancePool(instancePool);

    if (definition instanceof Concrete.FunctionDefinition) {
      FunctionDefinition functionDef = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(definition.getData());
      try {
        typecheckFunctionHeader(functionDef, (Concrete.FunctionDefinition) definition, localInstancePool, true, typechecked == null);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), definition));
      }
      return functionDef;
    } else
    if (definition instanceof Concrete.DataDefinition) {
      DataDefinition dataDef = typechecked != null ? (DataDefinition) typechecked : new DataDefinition(definition.getData());
      try {
        typecheckDataHeader(dataDef, (Concrete.DataDefinition) definition, localInstancePool, typechecked == null);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), definition));
      }
      if (dataDef.getSort() == null || dataDef.getSort().getPLevel().isInfinity()) {
        myVisitor.getErrorReporter().report(new TypecheckingError("Cannot infer the sort of a recursive data type", definition));
        if (typechecked == null) {
          dataDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
        }
      }
      return dataDef;
    } else {
      throw new IllegalStateException();
    }
  }

  public List<Clause> typecheckBody(Definition definition, Concrete.Definition def, Set<DataDefinition> dataDefinitions, boolean newDef) {
    if (definition instanceof FunctionDefinition) {
      try {
        return typecheckFunctionBody((FunctionDefinition) definition, (Concrete.FunctionDefinition) def, newDef);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), def));
      }
    } else
    if (definition instanceof DataDefinition) {
      try {
        if (!typecheckDataBody((DataDefinition) definition, (Concrete.DataDefinition) def, false, dataDefinitions, newDef) && newDef) {
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
      typecheckFunctionHeader(definition, def, localInstancePool, recursive, typechecked == null);
      return recursive && definition.getResultType() == null ? null : typecheckFunctionBody(definition, def, typechecked == null);
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
      typecheckDataHeader(definition, def, localInstancePool, typechecked == null);
      if (definition.status().headerIsOK()) {
        typecheckDataBody(definition, def, true, Collections.singleton(definition), typechecked == null);
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
    if (recursive) {
      myVisitor.getErrorReporter().report(new TypecheckingError("A class cannot be recursive", def));
      if (typechecked != null) {
        return null;
      }
    }

    ClassDefinition definition = typechecked != null ? (ClassDefinition) typechecked : new ClassDefinition(def.getData());
    if (typechecked == null) {
      myVisitor.getTypecheckingState().record(def.getData(), definition);
    }
    if (recursive) {
      definition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);

      for (Concrete.ClassField field : def.getFields()) {
        addField(field.getData(), definition, new PiExpression(Sort.STD, new HiddenTypedSingleDependentLink(false, "this", new ClassCallExpression(definition, Sort.STD)), new ErrorExpression(null, null))).setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
      }
    } else {
      try {
        typecheckClass(def, definition, typechecked == null);
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
      if (typechecked == null) {
        definition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
      }
    } else {
      try {
        typecheckInstance(def, definition, localInstancePool, typechecked == null);
      } catch (IncorrectExpressionException e) {
        myVisitor.getErrorReporter().report(new TypecheckingError(e.getMessage(), def));
      }
    }
    return null;
  }

  private Sort typeCheckParameters(List<? extends Concrete.Parameter> parameters, LinkList list, LocalInstancePool localInstancePool, Sort expectedSort, DependentLink oldParameters) {
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
        int numberOfParameters;
        boolean oldParametersOK = true;
        if (parameter instanceof Concrete.TelescopeParameter) {
          List<? extends Referable> referableList = ((Concrete.TelescopeParameter) parameter).getReferableList();
          List<String> names = ((Concrete.TelescopeParameter) parameter).getNames();
          param = oldParameters != null ? oldParameters : parameter(parameter.getExplicit(), names, paramResult);
          numberOfParameters = names.size();
          index += numberOfParameters;

          if (oldParameters == null) {
            int i = 0;
            for (DependentLink link = param; link.hasNext(); link = link.getNext(), i++) {
              myVisitor.getContext().put(referableList.get(i), link);
            }
          } else {
            for (int i = 0; i < names.size(); i++) {
              if (oldParameters.hasNext()) {
                myVisitor.getContext().put(referableList.get(i), oldParameters);
                myVisitor.getFreeBindings().add(oldParameters);
                oldParameters = oldParameters.getNext();
              } else {
                oldParametersOK = false;
                break;
              }
            }
          }
        } else {
          index++;
          numberOfParameters = 1;
          if (oldParameters != null) {
            param = oldParameters;
            if (oldParameters.hasNext()) {
              myVisitor.getFreeBindings().add(oldParameters);
              oldParameters = oldParameters.getNext();
            } else {
              oldParametersOK = false;
            }
          } else {
            param = parameter(parameter.getExplicit(), (String) null, paramResult);
          }
        }
        if (!oldParametersOK) {
          myVisitor.getErrorReporter().report(new TypecheckingError("Cannot typecheck definition. Try to clear cache", parameter));
          return null;
        }

        if (localInstancePool != null) {
          TCClassReferable classRef = typeParameter.getType().getUnderlyingClassReferable(false);
          TCClassReferable underlyingClassRef = classRef == null ? null : classRef.getUnderlyingTypecheckable();
          if (underlyingClassRef != null) {
            ClassDefinition classDef = (ClassDefinition) myVisitor.getTypecheckingState().getTypechecked(underlyingClassRef);
            if (classDef != null && !classDef.isRecord()) {
              ClassField classifyingField = classDef.getClassifyingField();
              int i = 0;
              for (DependentLink link = param; i < numberOfParameters; link = link.getNext(), i++) {
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

        if (oldParameters == null) {
          list.append(param);
          for (; param.hasNext(); param = param.getNext()) {
            myVisitor.getFreeBindings().add(param);
          }
        }
      } else {
        myVisitor.getErrorReporter().report(new ArgInferenceError(typeOfFunctionArg(++index), parameter, new Expression[0]));
        sort = null;
      }
    }

    if (oldParameters != null) {
      list.append(oldParameters);
    }
    return sort;
  }

  private void calculateParametersTypecheckingOrder(Definition definition) {
    List<DependentLink> parametersList;
    if (definition instanceof Constructor && ((Constructor) definition).getDataTypeParameters().hasNext()) {
      parametersList = new ArrayList<>(2);
      parametersList.add(((Constructor) definition).getDataTypeParameters());
      parametersList.add(definition.getParameters());
    } else {
      parametersList = Collections.singletonList(definition.getParameters());
    }

    LinkedHashSet<Binding> processed = new LinkedHashSet<>();
    for (DependentLink link : parametersList) {
      boolean isDataTypeParameter = parametersList.size() > 1 && link == parametersList.get(0);
      for (; link.hasNext(); link = link.getNext()) {
        if (processed.contains(link)) {
          continue;
        }
        if (link.isExplicit() && !isDataTypeParameter) {
          processed.add(link);
        } else {
          FreeVariablesClassifier classifier = new FreeVariablesClassifier(link);
          boolean isDataTypeParam = isDataTypeParameter;
          DependentLink link1 = link.getNext();
          boolean found = false;
          while (true) {
            if (!link1.hasNext()) {
              if (isDataTypeParam) {
                link1 = parametersList.get(1);
                isDataTypeParam = false;
              }
              if (!link1.hasNext()) {
                break;
              }
            }

            FreeVariablesClassifier.Result result = classifier.checkBinding(link1);
            if ((result == FreeVariablesClassifier.Result.GOOD || result == FreeVariablesClassifier.Result.BOTH) && processed.contains(link1)) {
              found = true;
              processed.add(link);
              break;
            }
            if (result == FreeVariablesClassifier.Result.GOOD && link1.isExplicit()) {
              found = true;
              processed.add(link);
              Set<Binding> freeVars = FreeVariablesCollector.getFreeVariables(link1.getTypeExpr());
              for (DependentLink link2 : parametersList) {
                for (; link2.hasNext() && link2 != link1; link2 = link2.getNext()) {
                  if (freeVars.contains(link2)) {
                    processed.add(link2);
                  }
                }
                if (link2 == link1) {
                  break;
                }
              }
              processed.add(link1);
              break;
            }

            link1 = link1.getNext();
          }

          if (!found) {
            processed.add(link);
          }
        }
      }
    }

    boolean needReorder = false;
    DependentLink link = parametersList.get(0);
    boolean isDataTypeParameter = parametersList.size() > 1;
    for (Binding binding : processed) {
      if (binding != link) {
        needReorder = true;
        break;
      }
      link = link.getNext();
      if (!link.hasNext() && isDataTypeParameter) {
        link = parametersList.get(1);
        isDataTypeParameter = false;
      }
    }

    if (needReorder) {
      Map<Binding,Integer> map = new HashMap<>();
      int i = 0;
      for (DependentLink link1 : parametersList) {
        for (; link1.hasNext(); link1 = link1.getNext()) {
          map.put(link1,i);
          i++;
        }
      }

      List<Integer> order = new ArrayList<>(processed.size());
      for (Binding binding : processed) {
        order.add(map.get(binding));
      }

      definition.setParametersTypecheckingOrder(order);
    }
  }

  private void typecheckFunctionHeader(FunctionDefinition typedDef, Concrete.FunctionDefinition def, LocalInstancePool localInstancePool, boolean recursive, boolean newDef) {
    LinkList list = new LinkList();

    boolean paramsOk = typeCheckParameters(def.getParameters(), list, localInstancePool, null, newDef ? null : typedDef.getParameters()) != null;
    Expression expectedType = null;
    Concrete.Expression resultType = def.getResultType();
    if (resultType != null) {
      ExpectedType expectedType1 = def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA;
      Type expectedTypeResult =
        def.getBody() instanceof Concrete.CoelimFunctionBody ? null :
        def.getBody() instanceof Concrete.TermFunctionBody ? myVisitor.checkType(resultType, expectedType1) : myVisitor.finalCheckType(resultType, expectedType1);
      if (expectedTypeResult != null) {
        expectedType = expectedTypeResult.getExpr();
      }
    }

    if (newDef) {
      myVisitor.getTypecheckingState().record(def.getData(), typedDef);
      typedDef.setParameters(list.getFirst());
      typedDef.setResultType(expectedType);
      typedDef.setStatus(paramsOk && expectedType != null ? Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING : Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      typedDef.setIsLemma(def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA);
      calculateParametersTypecheckingOrder(typedDef);
    }

    if (recursive && expectedType == null) {
      myVisitor.getErrorReporter().report(new TypecheckingError(def.getBody() instanceof Concrete.CoelimFunctionBody
        ? "Function defined by copattern matching cannot be recursive"
        : "Cannot infer the result type of a recursive function", def));
    }
  }

  private boolean checkForContravariantUniverses(Expression expr) {
    while (expr instanceof PiExpression) {
      for (DependentLink link = ((PiExpression) expr).getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (CheckForUniversesVisitor.findUniverse(link.getTypeExpr())) {
          return true;
        }
      }
      expr = ((PiExpression) expr).getCodomain();
    }
    if (expr instanceof UniverseExpression) {
      return false;
    }
    return CheckForUniversesVisitor.findUniverse(expr);
  }

  private boolean checkForUniverses(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      if (checkForContravariantUniverses(link.getTypeExpr())) {
        return true;
      }
    }
    return false;
  }

  private List<Clause> typecheckFunctionBody(FunctionDefinition typedDef, Concrete.FunctionDefinition def, boolean newDef) {
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
          if (newDef) {
            typedDef.setBody(typedBody);
            typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          }
          boolean conditionsResult = ConditionsChecking.check(typedBody, clauses, typedDef, def, myVisitor.getErrorReporter());
          if (newDef) {
            typedDef.setStatus(conditionsResult ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.HAS_ERRORS);
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
        Concrete.ReferenceExpression typeRefExpr = Concrete.getReferenceExpressionInType(def.getResultType());
        Referable typeRef = typeRefExpr == null ? null : typeRefExpr.getReferent();
        if (!(typeRef instanceof ClassReferable)) {
          myVisitor.getErrorReporter().report(new TypecheckingError("Expected a class", def.getResultType()));
          CheckTypeVisitor.Result result = myVisitor.finalCheckExpr(def.getResultType(), def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA, false);
          if (newDef && result != null) {
            typedDef.setResultType(result.expression);
            typedDef.setStatus(myVisitor.getStatus());
          }
        } else {
          typecheckCoClauses(newDef ? typedDef : null, def, def.getResultType(), body.getClassFieldImpls());
        }
        bodyIsOK = true;
      }
    } else {
      CheckTypeVisitor.Result termResult = myVisitor.finalCheckExpr(((Concrete.TermFunctionBody) body).getTerm(), expectedType, true);
      if (termResult != null) {
        if (termResult.expression != null) {
          if (newDef) {
            typedDef.setBody(new LeafElimTree(typedDef.getParameters(), termResult.expression));
          }
          clauses = Collections.emptyList();
        }
        if (termResult.expression instanceof FunCallExpression && ((FunCallExpression) termResult.expression).getDefinition().getBody() == null) {
          bodyIsOK = true;
          if (newDef) {
            typedDef.setBody(null);
          }
        }
        if (termResult.expression instanceof NewExpression) {
          bodyIsOK = true;
          if (newDef) {
            typedDef.setBody(null);
            typedDef.setResultType(((NewExpression) termResult.expression).getExpression());
          }
        } else {
          if (newDef) {
            typedDef.setResultType(termResult.type);
            if (def.getResultType() == null && def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA) {
              Expression typeType = termResult.type.getType();
              if (!(typeType instanceof UniverseExpression && ((UniverseExpression) typeType).getSort().equals(Sort.PROP))) {
                typedDef.setIsLemma(false);
                myVisitor.getErrorReporter().report(new TypeMismatchError(new UniverseExpression(Sort.PROP), typeType == null ? new ErrorExpression(null, null) : typeType, def));
              }
            }
          }
        }
      }
    }

    if (newDef) {
      typedDef.setStatus(typedDef.getResultType() == null ? Definition.TypeCheckingStatus.HEADER_HAS_ERRORS : !bodyIsOK && typedDef.getActualBody() == null ? Definition.TypeCheckingStatus.BODY_HAS_ERRORS : myVisitor.getStatus());
    }

    if (typedDef.status().headerIsOK() && def.getKind().isUse()) {
      Definition useParent = myVisitor.getTypecheckingState().getTypechecked(def.getUseParent());
      if (useParent instanceof DataDefinition || useParent instanceof ClassDefinition) {
        if (def.getKind() == Concrete.FunctionDefinition.Kind.COERCE) {
          if (def.getParameters().isEmpty()) {
            myVisitor.getErrorReporter().report(new TypecheckingError("\\coerce must have at least one parameter", def));
          } else {
            Definition paramDef = getExpressionDef(def.getParameters().get(def.getParameters().size() - 1).getType());
            DefCallExpression resultDefCall = typedDef.getResultType().checkedCast(DefCallExpression.class);
            Definition resultDef = resultDefCall == null ? null : resultDefCall.getDefinition();

            if ((resultDef == useParent) == (paramDef == useParent)) {
              myVisitor.getErrorReporter().report(new TypecheckingError("Either the last parameter or the result type (but not both) of \\coerce must be the parent definition", def));
            } else {
              if (newDef) {
                if (resultDef == useParent) {
                  useParent.getCoerceData().addCoerceFrom(paramDef, typedDef);
                } else {
                  useParent.getCoerceData().addCoerceTo(resultDef, typedDef);
                }
              }
            }
          }
        } else if (def.getKind() == Concrete.FunctionDefinition.Kind.LEVEL) {
          boolean ok = true;
          Expression type = null;
          DependentLink link = typedDef.getParameters();
          ExprSubstitution substitution = new ExprSubstitution();
          if (useParent instanceof DataDefinition) {
            List<Expression> dataCallArgs = new ArrayList<>();
            for (DependentLink dataLink = useParent.getParameters(); dataLink.hasNext(); dataLink = dataLink.getNext(), link = link.getNext()) {
              if (!link.hasNext()) {
                ok = false;
                break;
              }
              if (!Expression.compare(link.getTypeExpr(), dataLink.getTypeExpr().subst(substitution), Equations.CMP.EQ)) {
                ok = false;
                break;
              }
              ReferenceExpression refExpr = new ReferenceExpression(link);
              dataCallArgs.add(refExpr);
              substitution.add(dataLink, refExpr);
            }

            if (ok) {
              if (link.hasNext()) {
                type = new DataCallExpression((DataDefinition) useParent, Sort.STD, dataCallArgs);
              } else {
                ok = false;
              }
            }
          } else {
            type = new ClassCallExpression((ClassDefinition) useParent, Sort.STD);
          }

          int level = -2;
          if (ok) {
            List<DependentLink> parameters = new ArrayList<>();
            for (; link.hasNext(); link = link.getNext()) {
              parameters.add(link);
            }

            Expression resultType = typedDef.getResultType().getPiParameters(parameters, false);
            for (int i = 0; i < parameters.size(); i++) {
              link = parameters.get(i);
              if (link instanceof TypedDependentLink) {
                if (!Expression.compare(link.getTypeExpr(), type, Equations.CMP.EQ)) {
                  ok = false;
                  break;
                }
              }

              List<Expression> pathArgs = new ArrayList<>();
              pathArgs.add(type);
              pathArgs.add(new ReferenceExpression(link));
              i++;
              if (i >= parameters.size()) {
                ok = false;
                break;
              }
              link = parameters.get(i);
              if (!Expression.compare(link.getTypeExpr(), type, Equations.CMP.EQ)) {
                ok = false;
                break;
              }

              pathArgs.add(new ReferenceExpression(link));
              type = new FunCallExpression(Prelude.PATH_INFIX, Sort.STD, pathArgs);
              level++;
            }

            if (ok) {
              if (!Expression.compare(resultType, type, Equations.CMP.EQ)) {
                ok = false;
              }
            }
          }

          if (ok) {
            if (newDef) {
              if (useParent instanceof DataDefinition) {
                ((DataDefinition) useParent).setSort(new Sort(((DataDefinition) useParent).getSort().getPLevel(), new Level(level)));
              } else {
                ((ClassDefinition) useParent).setSort(new Sort(((ClassDefinition) useParent).getSort().getPLevel(), new Level(level)));
              }
            }
          } else {
            myVisitor.getErrorReporter().report(new TypecheckingError("\\use \\level has wrong format", def));
          }
        }
      } else {
        myVisitor.getErrorReporter().report(new TypecheckingError("\\use is allowed only in \\where block of \\data and \\class", def));
      }

      if (newDef && typedDef.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
        typedDef.setStatus(myVisitor.getStatus());
      }
    }

    if (newDef && (checkForUniverses(typedDef.getParameters()) || checkForContravariantUniverses(typedDef.getResultType()) || CheckForUniversesVisitor.findUniverse(typedDef.getBody()))) {
      typedDef.setHasUniverses(true);
    }

    return clauses;
  }

  private Definition getExpressionDef(Concrete.Expression expr) {
    Referable ref = expr.getUnderlyingReferable();
    return ref instanceof TCReferable ? myVisitor.getTypecheckingState().getTypechecked((TCReferable) ref) : null;
  }

  private void typecheckDataHeader(DataDefinition dataDefinition, Concrete.DataDefinition def, LocalInstancePool localInstancePool, boolean newDef) {
    LinkList list = new LinkList();

    Sort userSort = null;
    boolean paramsOk = typeCheckParameters(def.getParameters(), list, localInstancePool, null, newDef ? null : dataDefinition.getParameters()) != null;

    if (def.getUniverse() != null) {
      Type userTypeResult = myVisitor.finalCheckType(def.getUniverse(), ExpectedType.OMEGA);
      if (userTypeResult != null) {
        userSort = userTypeResult.getExpr().toSort();
        if (userSort == null) {
          myVisitor.getErrorReporter().report(new TypecheckingError("Expected a universe", def.getUniverse()));
        }
      }
    }

    if (!newDef) {
      return;
    }

    dataDefinition.setParameters(list.getFirst());
    dataDefinition.setSort(userSort);
    myVisitor.getTypecheckingState().record(def.getData(), dataDefinition);
    calculateParametersTypecheckingOrder(dataDefinition);

    if (!paramsOk) {
      dataDefinition.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          myVisitor.getTypecheckingState().rewrite(constructor.getData(), new Constructor(constructor.getData(), dataDefinition));
        }
      }
    } else {
      dataDefinition.setStatus(Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING);
    }
  }

  private boolean typecheckDataBody(DataDefinition dataDefinition, Concrete.DataDefinition def, boolean polyHLevel, Set<DataDefinition> dataDefinitions, boolean newDef) {
    if (newDef) {
      dataDefinition.getConstructors().clear();
    }

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
    if (newDef) {
      dataDefinition.setSort(inferredSort);
    }

    boolean dataOk = true;
    List<DependentLink> elimParams = Collections.emptyList();
    if (def.getEliminatedReferences() != null) {
      elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getConstructorClauses(), dataDefinition.getParameters(), myVisitor);
      if (elimParams == null) {
        dataOk = false;
      }
    }

    LocalErrorReporter errorReporter = myVisitor.getErrorReporter();
    LocalErrorReporterCounter countingErrorReporter = new LocalErrorReporterCounter(Error.Level.ERROR, errorReporter);
    myVisitor.setErrorReporter(countingErrorReporter);

    if (!def.getConstructorClauses().isEmpty()) {
      Map<Referable, Binding> context = myVisitor.getContext();
      Set<Binding> freeBindings = myVisitor.getFreeBindings();
      PatternTypechecking dataPatternTypechecking = new PatternTypechecking(myVisitor.getErrorReporter(), EnumSet.of(PatternTypechecking.Flag.CONTEXT_FREE));

      Set<TCReferable> notAllowedConstructors = new HashSet<>();
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          notAllowedConstructors.add(constructor.getData());
        }
      }

      Map<String, Referable> constructorNames = new HashMap<>();
      InstancePool instancePool = myVisitor.getInstancePool().getInstancePool();
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        myVisitor.setContext(new HashMap<>(context));
        myVisitor.setFreeBindings(new HashSet<>(freeBindings));

        // Typecheck patterns and compute free bindings
        boolean patternsOK = true;
        Pair<List<Pattern>, List<Expression>> result = null;
        if (clause.getPatterns() != null) {
          if (def.getEliminatedReferences() == null) {
            errorReporter.report(new TypecheckingError("Expected a constructor without patterns", clause));
            dataOk = false;
          }
          if (elimParams != null) {
            result = dataPatternTypechecking.typecheckPatterns(clause.getPatterns(), def.getParameters(), dataDefinition.getParameters(), elimParams, def, myVisitor);
            if (instancePool != null && result != null && result.proj2 != null) {
              ExprSubstitution substitution = new ExprSubstitution();
              DependentLink link = dataDefinition.getParameters();
              for (Expression expr : result.proj2) {
                substitution.add(link, expr);
                link = link.getNext();
              }
              myVisitor.getInstancePool().setInstancePool(instancePool.subst(substitution));
            }
            if (result != null && result.proj2 == null) {
              errorReporter.report(new TypecheckingError("This clause is redundant", clause));
              result = null;
            }
            if (result == null) {
              myVisitor.setContext(new HashMap<>(context));
              myVisitor.setFreeBindings(new HashSet<>(freeBindings));
              patternsOK = false;
            }
          }
        } else {
          if (def.getEliminatedReferences() != null) {
            errorReporter.report(new TypecheckingError("Expected constructors with patterns", clause));
            dataOk = false;
          }
        }

        // Process constructors
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          // Check that constructors do not refer to constructors defined later
          FreeReferablesVisitor visitor = new FreeReferablesVisitor(notAllowedConstructors);
          if (constructor.getResultType() != null) {
            if (constructor.getResultType().accept(visitor, null) != null) {
              myVisitor.getErrorReporter().report(new ConstructorReferenceError(constructor.getResultType()));
              constructor.setResultType(null);
            }
          }
          Iterator<Concrete.FunctionClause> it = constructor.getClauses().iterator();
          while (it.hasNext()) {
            Concrete.FunctionClause conClause = it.next();
            if (visitor.visitClause(conClause) != null) {
              myVisitor.getErrorReporter().report(new ConstructorReferenceError(conClause));
              it.remove();
            }
          }
          boolean constructorOK = patternsOK;
          if (visitor.visitParameters(constructor.getParameters()) != null) {
            myVisitor.getErrorReporter().report(new ConstructorReferenceError(constructor));
            constructorOK = false;
          }
          if (!constructorOK) {
            constructor.getParameters().clear();
            constructor.getEliminatedReferences().clear();
            constructor.getClauses().clear();
            constructor.setResultType(null);
          }
          notAllowedConstructors.remove(constructor.getData());

          // Typecheck constructors
          Patterns patterns = result == null ? null : new Patterns(result.proj1);
          Sort conSort = typecheckConstructor(constructor, patterns, dataDefinition, dataDefinitions, def.isTruncated() ? null : userSort, constructorNames, newDef);
          if (conSort == null) {
            dataOk = false;
            conSort = Sort.PROP;
          }

          inferredSort = inferredSort.max(conSort);
        }
      }
      myVisitor.getInstancePool().setInstancePool(instancePool);

      if (inferredSort == Sort.PROP) {
        boolean ok = true;
        for (int i = 0; i < dataDefinition.getConstructors().size(); i++) {
          Patterns patterns1 = dataDefinition.getConstructors().get(i).getPatterns();
          for (int j = i + 1; j < dataDefinition.getConstructors().size(); j++) {
            Patterns patterns2 = dataDefinition.getConstructors().get(j).getPatterns();
            if (patterns1 == null || patterns2 == null || patterns1.unify(patterns2, null, null)) {
              ok = false;
              break;
            }
          }
          if (!ok) {
            break;
          }
        }
        if (!ok) {
          inferredSort = inferredSort.max(Sort.SET0);
        }
      }
    }
    if (newDef) {
      dataDefinition.setStatus(dataOk ? Definition.TypeCheckingStatus.NO_ERRORS : Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
    }

    myVisitor.setErrorReporter(errorReporter);

    // Check if constructors pattern match on the interval
    for (Constructor constructor : dataDefinition.getConstructors()) {
      if (constructor.getBody() != null) {
        if (!dataDefinition.matchesOnInterval() && constructor.getBody() instanceof IntervalElim) {
          if (newDef) {
            dataDefinition.setMatchesOnInterval();
          }
          inferredSort = inferredSort.max(new Sort(inferredSort.getPLevel(), Level.INFINITY));
        }
      }
    }

    // Find covariant parameters
    if (newDef) {
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
        } else if (newDef) {
          dataDefinition.setIsTruncated(true);
        }
      }
    } else if (countingErrorReporter.getErrorsNumber() == 0 && userSort != null && !inferredSort.isLessOrEquals(userSort)) {
      String msg = "Actual universe " + inferredSort + " is not compatible with expected universe " + userSort;
      countingErrorReporter.report(new TypecheckingError(msg, def.getUniverse()));
    }

    if (newDef) {
      dataDefinition.setSort(countingErrorReporter.getErrorsNumber() == 0 && userSort != null ? userSort : inferredSort);
      if (dataDefinition.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
        dataDefinition.setStatus(myVisitor.getStatus());
      }

      boolean hasUniverses = checkForUniverses(dataDefinition.getParameters());
      if (!hasUniverses) {
        for (Constructor constructor : dataDefinition.getConstructors()) {
          for (DependentLink link = constructor.getParameters(); link.hasNext(); link = link.getNext()) {
            link = link.getNextTyped(null);
            if (CheckForUniversesVisitor.findUniverse(link.getTypeExpr())) {
              hasUniverses = true;
              break;
            }
          }
          if (hasUniverses) {
            break;
          }
          if (CheckForUniversesVisitor.findUniverse(constructor.getBody())) {
            hasUniverses = true;
            break;
          }
        }
      }
      if (hasUniverses) {
        dataDefinition.setHasUniverses(true);
      }
    }

    return countingErrorReporter.getErrorsNumber() == 0;
  }

  private Expression normalizePathExpression(Expression type, Constructor constructor, Concrete.SourceNode sourceNode) {
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    if (type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.PATH) {
      List<Expression> pathArgs = ((DataCallExpression) type).getDefCallArguments();
      Expression lamExpr = pathArgs.get(0).normalize(NormalizeVisitor.Mode.WHNF);
      if (lamExpr instanceof LamExpression) {
        Expression newType = normalizePathExpression(((LamExpression) lamExpr).getBody(), constructor, sourceNode);
        if (newType == null) {
          return null;
        } else {
          List<Expression> args = new ArrayList<>(3);
          args.add(new LamExpression(((LamExpression) lamExpr).getResultSort(), ((LamExpression) lamExpr).getParameters(), newType));
          args.add(pathArgs.get(1));
          args.add(pathArgs.get(2));
          return new DataCallExpression(Prelude.PATH, ((DataCallExpression) type).getSortArgument(), args);
        }
      } else {
        type = null;
      }
    }

    Expression expectedType = constructor.getDataTypeExpression(Sort.STD);
    if (type == null || !type.equals(expectedType)) {
      myVisitor.getErrorReporter().report(new TypecheckingError("Expected an iterated path type in " + expectedType, sourceNode));
      return null;
    }

    return type;
  }

  private Expression addAts(Expression expression, DependentLink param, Expression type) {
    while (type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.PATH) {
      List<Expression> args = new ArrayList<>(5);
      args.addAll(((DataCallExpression) type).getDefCallArguments());
      args.add(expression);
      LamExpression lamExpr = (LamExpression) ((DataCallExpression) type).getDefCallArguments().get(0);
      args.add(new ReferenceExpression(param));
      expression = new FunCallExpression(Prelude.AT, ((DataCallExpression) type).getSortArgument(), args);
      type = lamExpr.getBody();
      param = param.getNext();
    }
    return expression;
  }

  private Sort typecheckConstructor(Concrete.Constructor def, Patterns patterns, DataDefinition dataDefinition, Set<DataDefinition> dataDefinitions, Sort userSort, Map<String, Referable> definedConstructors, boolean newDef) {
    Constructor constructor = newDef ? new Constructor(def.getData(), dataDefinition) : null;
    if (constructor != null) {
      constructor.setPatterns(patterns);
    }
    Constructor oldConstructor = constructor != null ? constructor : (Constructor) myVisitor.getTypecheckingState().getTypechecked(def.getData());

    List<DependentLink> elimParams = null;
    Expression constructorType = null;
    LinkList list = new LinkList();
    Sort sort;

    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myVisitor.getFreeBindings())) {
      try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myVisitor.getContext())) {
        if (constructor != null) {
          myVisitor.getTypecheckingState().rewrite(def.getData(), constructor);
          dataDefinition.addConstructor(constructor);
        }
        addName(definedConstructors, def.getData());

        sort = typeCheckParameters(def.getParameters(), list, null, userSort, newDef ? null : oldConstructor.getParameters());

        int index = 0;
        for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext(), index++) {
          link = link.getNextTyped(null);
          if (!checkPositiveness(link.getTypeExpr(), index, def.getParameters(), def, myVisitor.getErrorReporter(), dataDefinitions)) {
            if (constructor != null) {
              constructor.setParameters(EmptyDependentLink.getInstance());
            }
            return null;
          }
        }

        if (def.getResultType() != null) {
          Type resultType = myVisitor.finalCheckType(def.getResultType(), ExpectedType.OMEGA);
          if (resultType != null) {
            constructorType = normalizePathExpression(resultType.getExpr(), oldConstructor, def.getResultType());
          }
          def.setResultType(null);
        }

        if (constructor != null) {
          constructor.setParameters(list.getFirst());
        }

        if (!def.getClauses().isEmpty()) {
          elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getClauses(), list.getFirst(), myVisitor);
        }
      }
    }

    if (elimParams != null) {
      try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myVisitor.getFreeBindings())) {
        try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myVisitor.getContext())) {
          List<Clause> clauses = new ArrayList<>();
          Body body = new ElimTypechecking(myVisitor, oldConstructor.getDataTypeExpression(Sort.STD), EnumSet.of(PatternTypechecking.Flag.ALLOW_INTERVAL, PatternTypechecking.Flag.ALLOW_CONDITIONS)).typecheckElim(def.getClauses(), def, def.getParameters(), oldConstructor.getParameters(), elimParams, clauses);
          if (constructor != null) {
            constructor.setBody(body);
            constructor.setClauses(clauses);
            constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          }
          ConditionsChecking.check(body, clauses, oldConstructor, def, myVisitor.getErrorReporter());
        }
      }
    }

    if (constructor != null && constructorType != null) {
      int numberOfNewParameters = 0;
      for (Expression type = constructorType; type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.PATH; type = ((LamExpression) ((DataCallExpression) type).getDefCallArguments().get(0)).getBody()) {
        numberOfNewParameters++;
      }

      if (numberOfNewParameters != 0) {
        DependentLink newParam = new TypedDependentLink(true, "i" + numberOfNewParameters, Interval(), EmptyDependentLink.getInstance());
        for (int i = numberOfNewParameters - 1; i >= 1; i--) {
          newParam = new UntypedDependentLink("i" + i, newParam);
        }
        list.append(newParam);
        constructor.setParameters(list.getFirst());
        constructor.setNumberOfIntervalParameters(numberOfNewParameters);

        List<Pair<Expression,Expression>> pairs;
        ElimTree elimTree;
        if (constructor.getBody() instanceof IntervalElim) {
          pairs = ((IntervalElim) constructor.getBody()).getCases();
          for (int i = 0; i < pairs.size(); i++) {
            pairs.set(i, new Pair<>(addAts(pairs.get(i).proj1, newParam, constructorType), addAts(pairs.get(i).proj2, newParam, constructorType)));
          }
          elimTree = ((IntervalElim) constructor.getBody()).getOtherwise();
        } else {
          pairs = new ArrayList<>();
          elimTree = constructor.getBody() instanceof ElimTree ? (ElimTree) constructor.getBody() : null;
        }

        while (constructorType instanceof DataCallExpression && ((DataCallExpression) constructorType).getDefinition() == Prelude.PATH) {
          List<Expression> pathArgs = ((DataCallExpression) constructorType).getDefCallArguments();
          LamExpression lamExpr = (LamExpression) pathArgs.get(0);
          constructorType = lamExpr.getBody();
          pairs.add(new Pair<>(addAts(pathArgs.get(1), newParam, constructorType.subst(lamExpr.getParameters(), Left())), addAts(pathArgs.get(2), newParam, constructorType.subst(lamExpr.getParameters(), Right()))));
          constructorType = constructorType.subst(lamExpr.getParameters(), new ReferenceExpression(newParam));
          newParam = newParam.getNext();
        }

        constructor.setBody(new IntervalElim(list.getFirst(), pairs, elimTree));
      }
    }

    if (constructor != null) {
      constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
      calculateParametersTypecheckingOrder(constructor);
    }
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

  private void typecheckClass(Concrete.ClassDefinition def, ClassDefinition typedDef, boolean newDef) {
    if (newDef) {
      typedDef.clear();
      typedDef.setHasUniverses(true);
    }

    LocalErrorReporter errorReporter = myVisitor.getErrorReporter();
    boolean classOk = true;

    if (newDef) {
      typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    }

    List<GlobalReferable> alreadyImplementFields = new ArrayList<>();
    Concrete.SourceNode alreadyImplementedSourceNode = null;

    // Process super classes
    for (Concrete.ReferenceExpression aSuperClass : def.getSuperClasses()) {
      ClassDefinition superClass = myVisitor.referableToDefinition(aSuperClass.getReferent(), ClassDefinition.class, "Expected a class", aSuperClass);
      if (superClass == null) {
        continue;
      }

      if (newDef) {
        typedDef.addFields(superClass.getFields());
        typedDef.addSuperClass(superClass);
      }

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
    Map<String, Referable> fieldNames = new HashMap<>();
    for (Concrete.ClassField field : def.getFields()) {
      if (previousType == field.getResultType()) {
        if (newDef && previousField != null) {
          addField(field.getData(), typedDef, previousField.getType(Sort.STD)).setStatus(previousField.status());
        }
      } else {
        previousField = typecheckClassField(field, typedDef, newDef);
        previousType = field.getResultType();
      }

      addName(fieldNames, field.getData());
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
      if (newDef) {
        typedDef.setClassifyingField(classifyingField);
        if (classifyingField != null) {
          typedDef.getCoerceData().addCoercingField(classifyingField);
        }
      }
    } else if (newDef) {
      typedDef.setRecord();
    }

    // Process implementations
    if (!typedDef.getSuperClasses().isEmpty() || !def.getImplementations().isEmpty()) {
      Map<ClassField,Concrete.ClassFieldImpl> implementedHere = new HashMap<>();
      for (Concrete.ClassFieldImpl classFieldImpl : def.getImplementations()) {
        ClassField field = myVisitor.referableToClassField(classFieldImpl.getImplementedField(), classFieldImpl);
        if (field == null) {
          classOk = false;
          continue;
        }
        boolean isFieldAlreadyImplemented;
        if (newDef) {
          isFieldAlreadyImplemented = typedDef.isImplemented(field);
        } else if (implementedHere.containsKey(field)) {
          isFieldAlreadyImplemented = true;
        } else {
          isFieldAlreadyImplemented = false;
          for (ClassDefinition superClass : typedDef.getSuperClasses()) {
            if (superClass.isImplemented(field)) {
              isFieldAlreadyImplemented = true;
              break;
            }
          }
        }
        if (isFieldAlreadyImplemented) {
          classOk = false;
          alreadyImplementFields.add(field.getReferable());
          alreadyImplementedSourceNode = classFieldImpl;
        } else {
          implementedHere.put(field, classFieldImpl);
        }
      }

      // Check for cycles in implementations
      DFS dfs = new DFS(typedDef, implementedHere);
      Set<ClassField> allFields = new HashSet<>(implementedHere.keySet());
      for (ClassDefinition superClass : typedDef.getSuperClasses()) {
        allFields.addAll(superClass.getFields());
      }
      Deque<ClassField> fieldsToCheck = new ArrayDeque<>(allFields);
      while (!fieldsToCheck.isEmpty()) {
        ClassField field = fieldsToCheck.pop();
        List<ClassField> cycle = dfs.findCycle(field);
        if (cycle != null) {
          errorReporter.report(CycleError.fromTypechecked(cycle, def));
          fieldsToCheck.removeAll(cycle);
          for (ClassField dep : cycle) {
            typedDef.removeImplementation(dep);
            implementedHere.remove(dep);
          }
          fieldsToCheck.add(field);
        }
      }

      // Typecheck implementations
      if (newDef) {
        typedDef.updateSort();
      }

      for (ClassField field : typedDef.getFields()) {
        Concrete.ClassFieldImpl classFieldImpl = implementedHere.get(field);
        if (classFieldImpl == null) {
          continue;
        }

        SingleDependentLink parameter = new HiddenTypedSingleDependentLink(false, "this", new ClassCallExpression(typedDef, Sort.STD));
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

        if (newDef) {
          typedDef.implementField(field, new LamExpression(Sort.STD, parameter, result == null ? new ErrorExpression(null, null) : result.expression));
        }
        myVisitor.getContext().clear();
        myVisitor.getFreeBindings().clear();
      }
    }

    if (!alreadyImplementFields.isEmpty()) {
      errorReporter.report(new FieldsImplementationError(true, alreadyImplementFields, alreadyImplementFields.size() > 1 ? def : alreadyImplementedSourceNode));
    }

    if (newDef) {
      typedDef.setStatus(!classOk ? Definition.TypeCheckingStatus.BODY_HAS_ERRORS : myVisitor.getStatus());
      typedDef.updateSort();

      typedDef.setHasUniverses(false);
      for (ClassField field : typedDef.getFields()) {
        if (!typedDef.isImplemented(field) && CheckForUniversesVisitor.findUniverse(field.getType(Sort.STD).getCodomain())) {
          typedDef.setHasUniverses(true);
          break;
        }
      }
    }
}

  private void addName(Map<String, Referable> names, TCReferable data) {
    Referable prev = names.putIfAbsent(data.textRepresentation(), data);
    if (prev != null) {
      myVisitor.getErrorReporter().report(new DuplicateNameError(Error.Level.ERROR, data, prev));
    }
  }

  private static class DFS {
    private final ClassDefinition classDef;
    private final Map<ClassField, Concrete.ClassFieldImpl> implementedHere;
    private final Map<ClassField, Boolean> state = new HashMap<>();
    private final Map<ClassField, Set<ClassField>> references = new HashMap<>();

    private DFS(ClassDefinition classDef, Map<ClassField, Concrete.ClassFieldImpl> implementedHere) {
      this.classDef = classDef;
      this.implementedHere = implementedHere;
    }

    List<ClassField> findCycle(ClassField field) {
      List<ClassField> cycle = dfs(field);
      if (cycle != null) {
        if (cycle.size() > 1) {
          cycle.remove(cycle.size() - 1);
          Collections.reverse(cycle);
        }
        state.entrySet().removeIf(entry -> !entry.getValue());
        for (ClassField dep : cycle) {
          references.put(dep, Collections.emptySet());
        }
      }
      return cycle;
    }

    private List<ClassField> dfs(ClassField field) {
      if (Boolean.TRUE.equals(state.putIfAbsent(field, false))) {
        return null;
      }

      Set<TCReferable> fieldReferables = new HashSet<>();
      for (ClassField classField : classDef.getFields()) {
        fieldReferables.add(classField.getReferable());
      }
      Set<ClassField> deps = references.computeIfAbsent(field, f -> {
        LamExpression impl = classDef.getImplementation(field);
        if (impl != null) {
          return FieldsCollector.getFields(impl.getBody(), classDef.getFields());
        }
        Concrete.ClassFieldImpl classFieldImpl = implementedHere.get(field);
        if (classFieldImpl != null) {
          Set<TCReferable> refs = ReferablesCollector.getReferables(((Concrete.LamExpression) classFieldImpl.implementation).body, fieldReferables);
          Set<ClassField> fieldDeps = new HashSet<>();
          for (ClassField classField : classDef.getFields()) {
            if (refs.contains(classField.getReferable())) {
              fieldDeps.add(classField);
            }
          }
          return fieldDeps;
        }
        return Collections.emptySet();
      });

      for (ClassField dep : deps) {
        Boolean st = state.get(dep);
        if (st == null) {
          List<ClassField> cycle = dfs(dep);
          if (cycle != null) {
            if (cycle.size() == 1 || cycle.get(0) != cycle.get(cycle.size() - 1)) {
              cycle.add(dep);
            }
            return cycle;
          }
        } else if (!st) {
          List<ClassField> cycle = new ArrayList<>();
          cycle.add(dep);
          return cycle;
        }
      }

      state.put(field, true);
      return null;
    }
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

  private ClassField typecheckClassField(Concrete.ClassField def, ClassDefinition parentClass, boolean newDef) {
    Type typeResult = myVisitor.finalCheckType(def.getResultType(), def.getKind() == ClassFieldKind.PROPERTY ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA);
    PiExpression piType = checkFieldType(typeResult, parentClass);
    if (piType == null) {
      TypedSingleDependentLink param = new HiddenTypedSingleDependentLink(false, "this", new ClassCallExpression(parentClass, Sort.STD));
      if (typeResult == null) {
        piType = new PiExpression(Sort.STD, param, new ErrorExpression(null, null));
      } else {
        myVisitor.getErrorReporter().report(new TypecheckingError("Internal error: class field must have a function type", def));
        piType = new PiExpression(typeResult.getSortOfType(), param, typeResult.getExpr());
        typeResult = null;
      }
    }

    if (!newDef) {
      return null;
    }

    boolean isProperty;
    if (def.getKind() == ClassFieldKind.ANY) {
      Expression universe = piType.getCodomain().getType();
      isProperty = universe instanceof UniverseExpression && ((UniverseExpression) universe).getSort().isProp();
    } else {
      isProperty = def.getKind() == ClassFieldKind.PROPERTY;
    }

    ClassField typedDef = addField(def.getData(), parentClass, piType);
    if (isProperty) {
      typedDef.setIsProperty();
    }
    if (typeResult == null) {
      typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
    }
    return typedDef;
  }

  private ClassField addField(TCFieldReferable fieldRef, ClassDefinition parentClass, PiExpression piType) {
    ClassField typedDef = new ClassField(fieldRef, parentClass, piType);
    myVisitor.getTypecheckingState().rewrite(fieldRef, typedDef);
    parentClass.addField(typedDef);
    parentClass.addPersonalField(typedDef);
    return typedDef;
  }

  private static boolean implementField(ClassField classField, LamExpression implementation, ClassDefinition classDef, List<GlobalReferable> alreadyImplemented) {
    LamExpression oldImpl = classDef.implementField(classField, implementation);
    if (oldImpl != null && !classField.isProperty() && !oldImpl.substArgument(new ReferenceExpression(implementation.getParameters())).equals(implementation.getBody())) {
      alreadyImplemented.add(classField.getReferable());
      return false;
    } else {
      return true;
    }
  }

  private ClassCallExpression typecheckCoClauses(FunctionDefinition typedDef, Concrete.Definition def, Concrete.Expression resultType, List<Concrete.ClassFieldImpl> classFieldImpls) {
    CheckTypeVisitor.Result result = myVisitor.finalCheckExpr(Concrete.ClassExtExpression.make(def.getData(), resultType, classFieldImpls), ExpectedType.OMEGA, false);
    if (result == null || !(result.expression instanceof ClassCallExpression)) {
      return null;
    }

    ClassCallExpression typecheckedResultType = (ClassCallExpression) result.expression;
    myVisitor.checkAllImplemented(typecheckedResultType, def);
    if (typedDef != null) {
      typedDef.setResultType(typecheckedResultType);
      typedDef.setStatus(myVisitor.getStatus());
    }
    return typecheckedResultType;
  }

  private void typecheckInstance(Concrete.Instance def, FunctionDefinition typedDef, LocalInstancePool localInstancePool, boolean newDef) {
    LinkList list = new LinkList();
    boolean paramsOk = typeCheckParameters(def.getParameters(), list, localInstancePool, null, newDef ? null : typedDef.getParameters()) != null;
    if (newDef) {
      typedDef.setParameters(list.getFirst());
      calculateParametersTypecheckingOrder(typedDef);
      typedDef.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
    }
    if (!paramsOk) {
      return;
    }

    if (!(def.getReferenceInType() instanceof ClassReferable)) {
      myVisitor.getErrorReporter().report(new TypecheckingError("Expected a class", def.getResultType()));
      return;
    }

    ClassCallExpression typecheckedResultType = typecheckCoClauses(newDef ? typedDef : null, def, def.getResultType(), def.getClassFieldImpls());
    if (typecheckedResultType == null) {
      return;
    }

    if (newDef && (checkForUniverses(typedDef.getParameters()) || checkForContravariantUniverses(typecheckedResultType))) {
      typedDef.setHasUniverses(true);
    }

    ClassField classifyingField = typecheckedResultType.getDefinition().getClassifyingField();
    if (classifyingField != null) {
      Expression classifyingExpr = typecheckedResultType.getImplementationHere(classifyingField);
      Set<SingleDependentLink> params = new LinkedHashSet<>();
      while (classifyingExpr instanceof LamExpression) {
        for (SingleDependentLink link = ((LamExpression) classifyingExpr).getParameters(); link.hasNext(); link = link.getNext()) {
          params.add(link);
        }
        classifyingExpr = ((LamExpression) classifyingExpr).getBody();
      }

      boolean ok = classifyingExpr == null || classifyingExpr instanceof ErrorExpression || classifyingExpr instanceof DataCallExpression || classifyingExpr instanceof ClassCallExpression || classifyingExpr instanceof UniverseExpression && params.isEmpty() || classifyingExpr instanceof IntegerExpression && params.isEmpty();
      if (classifyingExpr instanceof DataCallExpression) {
        DataCallExpression dataCall = (DataCallExpression) classifyingExpr;
        if (dataCall.getDefCallArguments().size() < params.size()) {
          ok = false;
        } else {
          int i = dataCall.getDefCallArguments().size() - params.size();
          for (SingleDependentLink param : params) {
            if (!(dataCall.getDefCallArguments().get(i) instanceof ReferenceExpression && ((ReferenceExpression) dataCall.getDefCallArguments().get(i)).getBinding() == param)) {
              ok = false;
              break;
            }
            i++;
          }
          if (ok) {
            for (i = 0; i < dataCall.getDefCallArguments().size() - params.size(); i++) {
              if (dataCall.getDefCallArguments().get(i).findBinding(params) != null) {
                ok = false;
                break;
              }
            }
          }
        }
      } else if (classifyingExpr instanceof ClassCallExpression) {
        Map<ClassField, Expression> implemented = ((ClassCallExpression) classifyingExpr).getImplementedHere();
        if (implemented.size() < params.size()) {
          ok = false;
        } else {
          int i = 0;
          ClassDefinition classDef = ((ClassCallExpression) classifyingExpr).getDefinition();
          Iterator<SingleDependentLink> it = params.iterator();
          for (ClassField field : classDef.getFields()) {
            Expression implementation = implemented.get(field);
            if (implementation != null) {
              if (i < implemented.size() - params.size()) {
                if (implementation.findBinding(params) != null) {
                  ok = false;
                  break;
                }
                i++;
              } else {
                if (!(implementation instanceof ReferenceExpression && it.hasNext() && ((ReferenceExpression) implementation).getBinding() == it.next())) {
                  ok = false;
                  break;
                }
              }
            } else {
              if (i >= implemented.size() - params.size()) {
                break;
              }
              if (!classDef.isImplemented(field)) {
                ok = false;
                break;
              }
            }
          }
        }
      }
      if (!ok) {
        myVisitor.getErrorReporter().report(new TypecheckingError(Error.Level.ERROR, "Classifying field must be either a universe, or a class, or a partially applied data", def.getResultType()));
      }
    }

    if (newDef) {
      typedDef.setStatus(myVisitor.getStatus());
    }
  }
}
