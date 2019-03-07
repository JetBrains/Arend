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
import org.arend.core.expr.visitor.GoodThisParametersVisitor;
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
import org.arend.typechecking.implicitargs.equations.DummyEquations;
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
import org.arend.util.Pair;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

public class DefinitionTypechecking implements ConcreteDefinitionVisitor<Boolean, List<Clause>> {
  private CheckTypeVisitor myVisitor;
  private LocalErrorReporter myErrorReporter;
  private GlobalInstancePool myInstancePool;

  public DefinitionTypechecking(CheckTypeVisitor visitor) {
    myVisitor = visitor;
    myErrorReporter = visitor == null ? null : visitor.getErrorReporter();
    myInstancePool = visitor == null ? null : visitor.getInstancePool();
  }

  public void setVisitor(CheckTypeVisitor visitor) {
    myVisitor = visitor;
    myErrorReporter = visitor.getErrorReporter();
    myInstancePool = visitor.getInstancePool();
  }

  public Definition typecheckHeader(Definition typechecked, GlobalInstancePool instancePool, Concrete.Definition definition, boolean recursive) {
    LocalInstancePool localInstancePool = new LocalInstancePool(myVisitor);
    instancePool.setInstancePool(localInstancePool);
    myVisitor.setInstancePool(instancePool);

    if (definition instanceof Concrete.FunctionDefinition) {
      FunctionDefinition functionDef = typechecked != null ? (FunctionDefinition) typechecked : new FunctionDefinition(definition.getData());
      try {
        typecheckFunctionHeader(functionDef, (Concrete.FunctionDefinition) definition, localInstancePool, recursive, typechecked == null);
      } catch (IncorrectExpressionException e) {
        myErrorReporter.report(new TypecheckingError(e.getMessage(), definition));
      }
      return functionDef;
    } else
    if (definition instanceof Concrete.DataDefinition) {
      DataDefinition dataDef = typechecked != null ? (DataDefinition) typechecked : new DataDefinition(definition.getData());
      try {
        typecheckDataHeader(dataDef, (Concrete.DataDefinition) definition, localInstancePool, typechecked == null);
      } catch (IncorrectExpressionException e) {
        myErrorReporter.report(new TypecheckingError(e.getMessage(), definition));
      }
      if (dataDef.getSort() == null || dataDef.getSort().getPLevel().isInfinity()) {
        myErrorReporter.report(new TypecheckingError("Cannot infer the sort of a recursive data type", definition));
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
        myErrorReporter.report(new TypecheckingError(e.getMessage(), def));
      }
    } else
    if (definition instanceof DataDefinition) {
      try {
        if (!typecheckDataBody((DataDefinition) definition, (Concrete.DataDefinition) def, false, dataDefinitions, newDef) && newDef) {
          definition.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
        }
      } catch (IncorrectExpressionException e) {
        myErrorReporter.report(new TypecheckingError(e.getMessage(), def));
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
      myErrorReporter.report(new TypecheckingError(e.getMessage(), def));
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
      myErrorReporter.report(new TypecheckingError(e.getMessage(), def));
      return null;
    }
    return null;
  }

  @Override
  public List<Clause> visitClass(Concrete.ClassDefinition def, Boolean recursive) {
    Definition typechecked = prepare(def);

    if (recursive) {
      myErrorReporter.report(new TypecheckingError("A class cannot be recursive", def));
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
        addField(field.getData(), definition, new PiExpression(Sort.STD, new TypedSingleDependentLink(false, "this", new ClassCallExpression(definition, Sort.STD), true), new ErrorExpression(null, null)), null).setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
      }
    } else {
      try {
        typecheckClass(def, definition, typechecked == null);
      } catch (IncorrectExpressionException e) {
        myErrorReporter.report(new TypecheckingError(e.getMessage(), def));
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
          List<? extends Referable> referableList = parameter.getReferableList();
          List<String> names = parameter.getNames();
          param = oldParameters != null
            ? oldParameters
            : referableList.size() == 1 && referableList.get(0) instanceof HiddenLocalReferable
              ? new TypedDependentLink(parameter.getExplicit(), names.get(0), paramResult, true, EmptyDependentLink.getInstance())
              : parameter(parameter.getExplicit(), names, paramResult);
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
          myErrorReporter.report(new TypecheckingError("Cannot typecheck definition. Try to clear cache", parameter));
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
                //   myErrorReporter.report(new DuplicateInstanceError(oldInstance, reference, parameter));
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
        myErrorReporter.report(new ArgInferenceError(typeOfFunctionArg(++index), parameter, new Expression[0]));
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

  private boolean isPropLevel(Concrete.Expression expression) {
    Concrete.Expression fun = expression instanceof Concrete.AppExpression ? ((Concrete.AppExpression) expression).getFunction() : expression;
    if (fun instanceof Concrete.ReferenceExpression) {
      Referable ref = ((Concrete.ReferenceExpression) fun).getReferent();
      if (ref instanceof TCReferable) {
        Definition typeDef = myVisitor.getTypecheckingState().getTypechecked((TCReferable) ref);
        if (typeDef != null) {
          for (Pair<? extends List<? extends Expression>, ? extends Sort> levelParameter : typeDef.getLevelParameters()) {
            if (levelParameter.proj1 == null && levelParameter.proj2 == Sort.PROP) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private void typecheckFunctionHeader(FunctionDefinition typedDef, Concrete.FunctionDefinition def, LocalInstancePool localInstancePool, boolean recursive, boolean newDef) {
    LinkList list = new LinkList();

    if (def.getResultTypeLevel() != null && !(def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA || def.getBody() instanceof Concrete.ElimFunctionBody)) {
      myErrorReporter.report(new TypecheckingError("\\level is allowed only for lemmas and functions defined by pattern matching", def.getResultTypeLevel()));
      def.setResultTypeLevel(null);
    }

    boolean paramsOk = typeCheckParameters(def.getParameters(), list, localInstancePool, null, newDef ? null : typedDef.getParameters()) != null;

    Expression expectedType = null;
    Concrete.Expression cResultType = def.getResultType();
    boolean isLemma = def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA || def.getKind() == Concrete.FunctionDefinition.Kind.LEVEL;
    if (cResultType != null) {
      ExpectedType typeExpectedType = def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA && def.getResultTypeLevel() == null && !isPropLevel(cResultType) ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA;
      Type expectedTypeResult;
      if (def.getBody() instanceof Concrete.CoelimFunctionBody) {
        expectedTypeResult = null;
      } else if (def.getBody() instanceof Concrete.TermFunctionBody && !recursive && !isLemma) {
        expectedTypeResult = myVisitor.checkType(cResultType, typeExpectedType);
      } else {
        expectedTypeResult = myVisitor.finalCheckType(cResultType, typeExpectedType);
      }
      if (expectedTypeResult != null) {
        expectedType = expectedTypeResult.getExpr();
      }
    }

    if (newDef) {
      myVisitor.getTypecheckingState().record(def.getData(), typedDef);
      typedDef.setParameters(list.getFirst());
      typedDef.setResultType(expectedType);
      typedDef.setStatus(paramsOk && expectedType != null ? Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING : Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      typedDef.setIsLemma(isLemma);
      calculateParametersTypecheckingOrder(typedDef);
    }

    if (recursive && expectedType == null) {
      myErrorReporter.report(new TypecheckingError(def.getBody() instanceof Concrete.CoelimFunctionBody
        ? "Function defined by copattern matching cannot be recursive"
        : "Cannot infer the result type of a recursive function", def));
    }

    if (paramsOk && def.getKind() == Concrete.FunctionDefinition.Kind.LEVEL) {
      Definition useParent = myVisitor.getTypecheckingState().getTypechecked(def.getUseParent());
      if (useParent instanceof DataDefinition || useParent instanceof ClassDefinition || useParent instanceof FunctionDefinition) {
        boolean ok = true;
        Set<ClassField> levelFields = null;
        Expression type = null;
        DependentLink link = typedDef.getParameters();
        if (useParent instanceof DataDefinition || useParent instanceof FunctionDefinition) {
          ExprSubstitution substitution = new ExprSubstitution();
          List<Expression> defCallArgs = new ArrayList<>();
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
            defCallArgs.add(refExpr);
            substitution.add(dataLink, refExpr);
          }

          if (ok) {
            if (link.hasNext()) {
              type = useParent instanceof DataDefinition
                ? new DataCallExpression((DataDefinition) useParent, Sort.STD, defCallArgs)
                : new FunCallExpression((FunctionDefinition) useParent, Sort.STD, defCallArgs);
            } else {
              ok = false;
            }
          }
        } else {
          ClassCallExpression classCall = null;
          DependentLink classCallLink = link;
          for (; classCallLink.hasNext(); classCallLink = classCallLink.getNext()) {
            classCallLink = classCallLink.getNextTyped(null);
            classCall = classCallLink.getTypeExpr().checkedCast(ClassCallExpression.class);
            if (classCall != null && classCall.getDefinition() == useParent && (!classCall.getDefinition().hasUniverses() || classCall.getSortArgument().equals(Sort.STD))) {
              break;
            }
          }
          if (classCall == null || !classCallLink.hasNext()) {
            ok = false;
          } else {
            Expression thisExpr = new ReferenceExpression(classCallLink);
            for (ClassField classField : classCall.getDefinition().getFields()) {
              Expression impl = classCall.getImplementationHere(classField);
              if (impl == null) {
                continue;
              }
              if (!(link.hasNext() && impl instanceof ReferenceExpression && ((ReferenceExpression) impl).getBinding() == link && classField.getType(Sort.STD).applyExpression(thisExpr).equals(link.getTypeExpr()))) {
                ok = false;
                break;
              }
              link = link.getNext();
            }
            if (!classCall.getImplementedHere().isEmpty()) {
              levelFields = new HashSet<>(classCall.getImplementedHere().keySet());
            }
            type = classCall;
          }
        }

        Integer level = myVisitor.getExpressionLevel(link, expectedType, ok ? type : null, DummyEquations.getInstance(), def);
        if (level != null && newDef) {
          if (useParent instanceof DataDefinition) {
            ((DataDefinition) useParent).setSort(level == -1 ? Sort.PROP : new Sort(((DataDefinition) useParent).getSort().getPLevel(), new Level(level)));
          } else if (useParent instanceof FunctionDefinition) {
            FunctionDefinition function = (FunctionDefinition) useParent;
            Sort sort = function.getResultType().toSort();
            if (sort == null) {
              myErrorReporter.report(new TypecheckingError("\\use \\level is allowed only for functions returning a type", def));
            } else {
              function.addLevelParameters(new Pair<>(null, level == -1 ? Sort.PROP : new Sort(sort.getPLevel(), new Level(level))));
            }
          } else {
            if (levelFields == null) {
              ((ClassDefinition) useParent).setSort(level == -1 ? Sort.PROP : new Sort(((ClassDefinition) useParent).getSort().getPLevel(), new Level(level)));
            } else {
              ((ClassDefinition) useParent).addLevel(levelFields, new Level(level));
            }
          }
        }
      }
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

  private Integer typecheckResultTypeLevel(Concrete.FunctionDefinition def, FunctionDefinition typedDef, boolean newDef) {
    return typecheckResultTypeLevel(def.getResultTypeLevel(), def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA, false, typedDef.getResultType(), typedDef, null, newDef);
  }

  private Integer typecheckResultTypeLevel(Concrete.Expression resultTypeLevel, boolean isLemma, boolean isProperty, Expression resultType, FunctionDefinition funDef, ClassField classField, boolean newDef) {
    if (resultTypeLevel != null) {
      CheckTypeVisitor.Result result = myVisitor.finalCheckExpr(resultTypeLevel, null, false);
      if (result != null && resultType != null) {
        Integer level = myVisitor.getExpressionLevel(EmptyDependentLink.getInstance(), result.type, resultType, DummyEquations.getInstance(), resultTypeLevel);
        if (level != null) {
          if ((isLemma || isProperty) && level != -1) {
            myErrorReporter.report(new TypecheckingError("The level of a " + (isLemma ? "lemma" : "property") + " must be \\Prop", resultTypeLevel));
            if (newDef && funDef != null) {
              funDef.setIsLemma(false);
            }
            if (isProperty) {
              return null;
            }
          }
          if (newDef) {
            if (funDef != null) {
              funDef.setResultTypeLevel(result.expression);
            }
            if (classField != null) {
              classField.setTypeLevel(result.expression);
            }
          }
        }
        return level;
      }
    }
    return null;
  }

  private void calculateGoodThisParameters(Constructor definition) {
    GoodThisParametersVisitor visitor;
    if (definition.getPatterns() == null) {
      visitor = new GoodThisParametersVisitor(definition.getParameters());
    } else {
      visitor = new GoodThisParametersVisitor(definition.getPatterns().getFirstBinding());
      visitor.visitParameters(definition.getParameters(), null);
    }
    visitor.visitBody(definition.getBody(), null);
    definition.setGoodThisParameters(visitor.getGoodParameters());
  }

  private void calculateTypeClassParameters(Concrete.ReferableDefinition refDef, Definition def) {
    List<Boolean> typeClassParameters = new ArrayList<>();
    for (Concrete.TypeParameter parameter : (refDef instanceof Concrete.Constructor ? ((Concrete.Constructor) refDef).getParameters() : Concrete.getParameters(refDef))) {
      Concrete.ReferenceExpression refExpr = Concrete.getReferenceExpressionInType(parameter.getType());
      boolean isTypeClass = refExpr != null && refExpr.getReferent() instanceof ClassReferable;
      for (int i = 0; i < parameter.getNumberOfParameters(); i++) {
        typeClassParameters.add(isTypeClass);
      }
    }

    for (Boolean isTypeClass : typeClassParameters) {
      if (isTypeClass) {
        def.setTypeClassParameters(typeClassParameters);
        return;
      }
    }
  }

  private List<Clause> typecheckFunctionBody(FunctionDefinition typedDef, Concrete.FunctionDefinition def, boolean newDef) {
    Expression expectedType = typedDef.getResultType();
    Integer resultTypeLevel = expectedType == null ? null : typecheckResultTypeLevel(def, typedDef, newDef);
    if (resultTypeLevel == null && expectedType != null) {
      resultTypeLevel = myVisitor.getExpressionLevel(expectedType);
    }

    GoodThisParametersVisitor goodThisParametersVisitor;
    if (newDef) {
      goodThisParametersVisitor = new GoodThisParametersVisitor(typedDef.getParameters());
      if (expectedType != null) {
        expectedType.accept(goodThisParametersVisitor, null);
      }
      if (typedDef.getResultTypeLevel() != null) {
        typedDef.getResultTypeLevel().accept(goodThisParametersVisitor, null);
      }
      typedDef.setGoodThisParameters(goodThisParametersVisitor.getGoodParameters());
    } else {
      goodThisParametersVisitor = null;
    }

    List<Clause> clauses = null;
    Concrete.FunctionBody body = def.getBody();
    boolean bodyIsOK = false;
    if (body instanceof Concrete.ElimFunctionBody) {
      if (expectedType != null) {
        Concrete.ElimFunctionBody elimBody = (Concrete.ElimFunctionBody) body;
        List<DependentLink> elimParams = ElimTypechecking.getEliminatedParameters(elimBody.getEliminatedReferences(), elimBody.getClauses(), typedDef.getParameters(), myVisitor);
        clauses = new ArrayList<>();
        EnumSet<PatternTypechecking.Flag> flags = EnumSet.of(PatternTypechecking.Flag.CHECK_COVERAGE, PatternTypechecking.Flag.CONTEXT_FREE, PatternTypechecking.Flag.ALLOW_INTERVAL, PatternTypechecking.Flag.ALLOW_CONDITIONS);
        Body typedBody = elimParams == null ? null : (def.getResultTypeLevel() == null
          ? new ElimTypechecking(myVisitor, expectedType, flags)
          : new ElimTypechecking(myVisitor, expectedType, flags, resultTypeLevel)
          ).typecheckElim(elimBody.getClauses(), def, def.getParameters(), typedDef.getParameters(), elimParams, clauses);
        if (typedBody != null) {
          if (newDef) {
            typedDef.setBody(typedBody);
            typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          }
          boolean conditionsResult = typedDef.isLemma() || ConditionsChecking.check(typedBody, clauses, typedDef, def, myErrorReporter);
          if (newDef && !conditionsResult) {
            typedDef.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          }
        } else {
          clauses = null;
        }
      } else {
        if (def.getResultType() == null) {
          myErrorReporter.report(new TypecheckingError("Cannot infer type of a function defined by pattern matching", def));
        }
      }
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      if (def.getResultType() == null) {
        myErrorReporter.report(new TypecheckingError("Cannot infer type of a function defined by copattern matching", def));
      } else {
        Concrete.ReferenceExpression typeRefExpr = Concrete.getReferenceExpressionInType(def.getResultType());
        Referable typeRef = typeRefExpr == null ? null : typeRefExpr.getReferent();
        if (!(typeRef instanceof ClassReferable)) {
          myErrorReporter.report(new TypecheckingError("Expected a class", def.getResultType()));
          CheckTypeVisitor.Result result = myVisitor.finalCheckExpr(def.getResultType(), def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA, false);
          if (newDef && result != null) {
            typedDef.setResultType(result.expression);
            typedDef.setStatus(myVisitor.getStatus());
          }
        } else {
          typecheckCoClauses(typedDef, def, def.getResultType(), def.getResultTypeLevel(), body.getClassFieldImpls(), newDef);
          typecheckResultTypeLevel(def, typedDef, newDef);
          clauses = Collections.emptyList();
        }
        bodyIsOK = true;
      }
    } else {
      CheckTypeVisitor.Result termResult = myVisitor.finalCheckExpr(((Concrete.TermFunctionBody) body).getTerm(), expectedType, true);
      if (termResult != null) {
        clauses = Collections.emptyList();
        if (termResult.expression != null) {
          if (newDef) {
            typedDef.setBody(new LeafElimTree(typedDef.getParameters(), termResult.expression));
          }
        }
        if (termResult.expression instanceof FunCallExpression && ((FunCallExpression) termResult.expression).getDefinition().getActualBody() == null && ((FunCallExpression) termResult.expression).getDefinition().status() != Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
          bodyIsOK = true;
          if (newDef) {
            typedDef.setBody(null);
          }
        }
        if (termResult.expression instanceof NewExpression) {
          bodyIsOK = true;
          if (newDef && (expectedType == null || !typedDef.isLemma())) {
            typedDef.setBody(null);
            typedDef.setResultType(((NewExpression) termResult.expression).getExpression());
          }
        } else {
          if (newDef && (expectedType == null || !typedDef.isLemma())) {
            typedDef.setResultType(termResult.type);
          }
          if (def.getResultType() == null && def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA) {
            Expression typeType = termResult.type.getType();
            if (!(typeType instanceof UniverseExpression && ((UniverseExpression) typeType).getSort().equals(Sort.PROP))) {
              if (newDef) {
                typedDef.setIsLemma(false);
              }
              myErrorReporter.report(new TypeMismatchError(new UniverseExpression(Sort.PROP), typeType == null ? new ErrorExpression(null, null) : typeType, def));
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
      if (useParent instanceof DataDefinition || useParent instanceof ClassDefinition || useParent instanceof FunctionDefinition) {
        if (def.getKind() == Concrete.FunctionDefinition.Kind.COERCE) {
          if (useParent instanceof FunctionDefinition) {
            myErrorReporter.report(new TypecheckingError("\\use \\coerce is allowed only in \\where block of \\data or \\class", def));
          } else {
            if (def.getParameters().isEmpty()) {
              myErrorReporter.report(new TypecheckingError("\\coerce must have at least one parameter", def));
            } else {
              Definition paramDef = getExpressionDef(def.getParameters().get(def.getParameters().size() - 1).getType());
              DefCallExpression resultDefCall = typedDef.getResultType().checkedCast(DefCallExpression.class);
              Definition resultDef = resultDefCall == null ? null : resultDefCall.getDefinition();

              if ((resultDef == useParent) == (paramDef == useParent)) {
                myErrorReporter.report(new TypecheckingError("Either the last parameter or the result type (but not both) of \\coerce must be the parent definition", def));
              } else {
                if (newDef) {
                  typedDef.setVisibleParameter(DependentLink.Helper.size(typedDef.getParameters()) - 1);
                  if (resultDef == useParent) {
                    useParent.getCoerceData().addCoerceFrom(paramDef, typedDef);
                  } else {
                    useParent.getCoerceData().addCoerceTo(resultDef, typedDef);
                  }
                }
              }
            }
          }
        }
      } else {
        myErrorReporter.report(new TypecheckingError("\\use is allowed only in \\where block of \\data, \\class, or \\func", def));
      }

      if (newDef && typedDef.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
        typedDef.setStatus(myVisitor.getStatus());
      }
    }

    if (newDef) {
      if (expectedType == null && typedDef.getResultType() != null) {
        typedDef.getResultType().accept(goodThisParametersVisitor, null);
      }
      if (typedDef.getResultType() == null) {
        typedDef.setResultType(new ErrorExpression(null, null));
      }

      ElimTree elimTree;
      if (typedDef.getActualBody() instanceof ElimTree) {
        elimTree = (ElimTree) typedDef.getActualBody();
      } else if (typedDef.getActualBody() instanceof IntervalElim) {
        elimTree = ((IntervalElim) typedDef.getActualBody()).getOtherwise();
      } else {
        elimTree = null;
      }

      if (elimTree != null) {
        goodThisParametersVisitor = new GoodThisParametersVisitor(elimTree, DependentLink.Helper.size(typedDef.getParameters()));
      } else {
        goodThisParametersVisitor.visitBody(typedDef.getActualBody(), null);
      }
      typedDef.setGoodThisParameters(goodThisParametersVisitor.getGoodParameters());
      calculateTypeClassParameters(def, typedDef);

      if (checkForUniverses(typedDef.getParameters()) || checkForContravariantUniverses(typedDef.getResultType()) || CheckForUniversesVisitor.findUniverse(typedDef.getBody())) {
        typedDef.setHasUniverses(true);
      }
    }

    if (def.getKind() == Concrete.FunctionDefinition.Kind.INSTANCE) {
      ClassCallExpression typecheckedResultType = typedDef.getResultType() instanceof ClassCallExpression ? (ClassCallExpression) typedDef.getResultType() : null;
      if (typecheckedResultType != null && !typecheckedResultType.getDefinition().isRecord()) {
        ClassField classifyingField = typecheckedResultType.getDefinition().getClassifyingField();
        Expression classifyingExpr;
        if (classifyingField != null) {
          classifyingExpr = typecheckedResultType.getImplementation(classifyingField, new NewExpression(typecheckedResultType));
          Set<SingleDependentLink> params = new LinkedHashSet<>();
          while (classifyingExpr instanceof LamExpression) {
            for (SingleDependentLink link = ((LamExpression) classifyingExpr).getParameters(); link.hasNext(); link = link.getNext()) {
              params.add(link);
            }
            classifyingExpr = ((LamExpression) classifyingExpr).getBody();
          }
          if (classifyingExpr != null) {
            classifyingExpr = classifyingExpr.normalize(NormalizeVisitor.Mode.WHNF);
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
            myErrorReporter.report(new TypecheckingError(Error.Level.ERROR, "Classifying field must be either a universe, or a class, or a partially applied data", def.getResultType() == null ? def : def.getResultType()));
          }
        } else {
          classifyingExpr = null;
        }

        boolean recOK = true;
        for (DependentLink link = typedDef.getParameters(); link.hasNext(); link = link.getNext()) {
          link = link.getNextTyped(null);
          Expression type = link.getTypeExpr();
          if (type instanceof ClassCallExpression && !((ClassCallExpression) type).getDefinition().isRecord()) {
            ClassField paramClassifyingField = ((ClassCallExpression) type).getDefinition().getClassifyingField();
            Expression classifyingImpl = paramClassifyingField == null ? null : ((ClassCallExpression) type).getImplementation(paramClassifyingField, new ReferenceExpression(link));
            if (classifyingImpl == null && paramClassifyingField != null) {
              classifyingImpl = FieldCallExpression.make(paramClassifyingField, Sort.STD, new ReferenceExpression(link));
            }
            if (classifyingImpl == null || classifyingExpr == null || compareExpressions(classifyingImpl, classifyingExpr) != -1) {
              myErrorReporter.report(new TypecheckingError("Class parameters of an instance must be classified by a subexpression of the classifying expression", def));
              recOK = false;
              break;
            }
          }
        }

        if (newDef) {
          typedDef.setStatus(recOK ? myVisitor.getStatus() : Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
        }
      } else if (!(typedDef.getResultType() instanceof ErrorExpression)) {
        myErrorReporter.report(new TypecheckingError("An instance must return a class", def.getResultType() == null ? def : def.getResultType()));
      }
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
          myErrorReporter.report(new TypecheckingError("Expected a universe", def.getUniverse()));
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
    GoodThisParametersVisitor goodThisParametersVisitor = new GoodThisParametersVisitor(dataDefinition.getParameters());
    dataDefinition.setGoodThisParameters(goodThisParametersVisitor.getGoodParameters());

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

    LocalErrorReporter errorReporter = myErrorReporter;
    LocalErrorReporterCounter countingErrorReporter = new LocalErrorReporterCounter(Error.Level.ERROR, errorReporter);
    myErrorReporter = countingErrorReporter;

    if (!def.getConstructorClauses().isEmpty()) {
      Map<Referable, Binding> context = myVisitor.getContext();
      Set<Binding> freeBindings = myVisitor.getFreeBindings();
      PatternTypechecking dataPatternTypechecking = new PatternTypechecking(myErrorReporter, EnumSet.of(PatternTypechecking.Flag.CONTEXT_FREE), myVisitor);

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
            result = dataPatternTypechecking.typecheckPatterns(clause.getPatterns(), def.getParameters(), dataDefinition.getParameters(), elimParams, def);
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
              myErrorReporter.report(new ConstructorReferenceError(constructor.getResultType()));
              constructor.setResultType(null);
            }
          }
          Iterator<Concrete.FunctionClause> it = constructor.getClauses().iterator();
          while (it.hasNext()) {
            Concrete.FunctionClause conClause = it.next();
            if (visitor.visitClause(conClause) != null) {
              myErrorReporter.report(new ConstructorReferenceError(conClause));
              it.remove();
            }
          }
          boolean constructorOK = patternsOK;
          if (visitor.visitParameters(constructor.getParameters()) != null) {
            myErrorReporter.report(new ConstructorReferenceError(constructor));
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

      if (inferredSort.equals(Sort.PROP)) {
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

    myErrorReporter = errorReporter;

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
          errorReporter.report(new TypecheckingError(Error.Level.WARNING, msg, def.getUniverse() == null ? def : def.getUniverse()));
        } else if (newDef) {
          dataDefinition.setIsTruncated(true);
        }
      }
    } else if (countingErrorReporter.getErrorsNumber() == 0 && userSort != null && !inferredSort.isLessOrEquals(userSort)) {
      String msg = "Actual universe " + inferredSort + " is not compatible with expected universe " + userSort;
      countingErrorReporter.report(new TypecheckingError(msg, def.getUniverse() == null ? def : def.getUniverse()));
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

    if (newDef) {
      for (Constructor constructor : dataDefinition.getConstructors()) {
        goodThisParametersVisitor.visitParameters(constructor.getParameters(), null);
        goodThisParametersVisitor.visitBody(constructor.getBody(), null);
      }
      dataDefinition.setGoodThisParameters(goodThisParametersVisitor.getGoodParameters());
      calculateTypeClassParameters(def, dataDefinition);
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
      myErrorReporter.report(new TypecheckingError("Expected an iterated path type in " + expectedType, sourceNode));
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
          if (!checkPositiveness(link.getTypeExpr(), index, def.getParameters(), def, myErrorReporter, dataDefinitions)) {
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
          ConditionsChecking.check(body, clauses, oldConstructor, def, myErrorReporter);
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
      calculateGoodThisParameters(constructor);
      calculateTypeClassParameters(def, constructor);
    }
    return sort;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkPositiveness(Expression type, int index, List<? extends Concrete.Parameter> parameters, Concrete.Constructor constructor, LocalErrorReporter errorReporter, Set<? extends Variable> variables) {
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
      i += parameter1.getNumberOfParameters();
      if (i > index) {
        parameter = parameter1;
        break;
      }
    }

    errorReporter.report(new NonPositiveDataError((DataDefinition) def, constructor, parameter == null ? constructor : parameter));
    return false;
  }

  private static class LocalInstance {
    final ClassDefinition classDefinition;
    final TCClassReferable classReferable;
    final ClassField instanceField;
    final Concrete.ClassField concreteField;

    LocalInstance(ClassDefinition classDefinition, TCClassReferable classReferable, ClassField instanceField, Concrete.ClassField concreteField) {
      this.classDefinition = classDefinition;
      this.classReferable = classReferable;
      this.instanceField = instanceField;
      this.concreteField = concreteField;
    }
  }

  private void typecheckClass(Concrete.ClassDefinition def, ClassDefinition typedDef, boolean newDef) {
    if (newDef) {
      typedDef.clear();
      typedDef.setHasUniverses(true);
    }

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
    List<LocalInstance> localInstances = new ArrayList<>();
    for (Concrete.ClassField field : def.getFields()) {
      if (previousType == field.getResultType()) {
        if (newDef && previousField != null) {
          addField(field.getData(), typedDef, previousField.getType(Sort.STD), previousField.getTypeLevel()).setStatus(previousField.status());
        }
      } else {
        previousType = field.getResultType();
        previousField = typecheckClassField(field, typedDef, localInstances, newDef);

        if (field.getData().isParameterField() && !field.getData().isExplicitField()) {
          TCClassReferable classRef = previousType.getUnderlyingClassReferable(false);
          TCClassReferable underlyingClassRef = classRef == null ? null : classRef.getUnderlyingTypecheckable();
          if (underlyingClassRef != null) {
            ClassDefinition classDef = (ClassDefinition) myVisitor.getTypecheckingState().getTypechecked(underlyingClassRef);
            if (classDef != null && !classDef.isRecord()) {
              ClassField typecheckedField = previousField != null ? previousField : (ClassField) myVisitor.getTypecheckingState().getTypechecked(field.getData());
              localInstances.add(new LocalInstance(classDef, classRef, typecheckedField, field));
            }
          }
        }
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
      if (classifyingField != null && def.isForcedCoercingField() && def.getCoercingField() != null) {
        myErrorReporter.report(new TypecheckingError("Field '" + def.getCoercingField().textRepresentation() + "' cannot be a classifying field since the class already has one: '" + classifyingField.getName() + "'", def));
      }
      if (classifyingField == null && def.getCoercingField() != null) {
        Definition definition = myVisitor.getTypecheckingState().getTypechecked(def.getCoercingField());
        if (definition instanceof ClassField && ((ClassField) definition).getParentClass().equals(typedDef)) {
          classifyingField = (ClassField) definition;
        } else {
          myErrorReporter.report(new TypecheckingError("Internal error: coercing field must be a field belonging to the class", def));
        }
      }
      if (newDef) {
        typedDef.setClassifyingField(classifyingField);
        if (classifyingField != null) {
          classifyingField.setHideable(true);
          classifyingField.setType(classifyingField.getType(Sort.STD).normalize(NormalizeVisitor.Mode.WHNF));
          typedDef.getCoerceData().addCoercingField(classifyingField);
        }
      }
    } else {
      if (def.isForcedCoercingField()) {
        myErrorReporter.report(new TypecheckingError("Records cannot have classifying fields", def));
      }
      if (newDef) {
        typedDef.setRecord();
      }
    }

    // Process implementations
    if (!typedDef.getSuperClasses().isEmpty() || !def.getImplementations().isEmpty()) {
      Map<ClassField,Concrete.ClassFieldImpl> implementedHere = new LinkedHashMap<>();
      ClassField lastField = null;
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
          lastField = field;
        }
      }

      // Check for cycles in implementations
      DFS dfs = new DFS(typedDef);
      if (implementedHere.isEmpty()) {
        dfs.setImplementedFields(Collections.emptySet());
      }
      List<ClassField> cycle = null;
      for (ClassField field : typedDef.getFields()) {
        cycle = dfs.findCycle(field);
        if (cycle != null) {
          myErrorReporter.report(CycleError.fromTypechecked(cycle, def));
          implementedHere.clear();
          break;
        }
      }

      // Typecheck implementations
      if (newDef && !implementedHere.isEmpty()) {
        typedDef.updateSort();
      }

      for (Map.Entry<ClassField, Concrete.ClassFieldImpl> entry : implementedHere.entrySet()) {
        SingleDependentLink parameter = new TypedSingleDependentLink(false, "this", new ClassCallExpression(typedDef, Sort.STD), true);
        Concrete.LamExpression lamImpl = (Concrete.LamExpression) entry.getValue().implementation;
        CheckTypeVisitor.Result result;
        if (lamImpl != null) {
          myVisitor.getContext().put(lamImpl.getParameters().get(0).getReferableList().get(0), parameter);
          myVisitor.getFreeBindings().add(parameter);
          PiExpression fieldType = entry.getKey().getType(Sort.STD);
          setClassLocalInstancePool(localInstances, parameter);
          result = myVisitor.finalCheckExpr(lamImpl.body, fieldType.getCodomain().subst(fieldType.getParameters(), new ReferenceExpression(parameter)), false);
          myInstancePool.setInstancePool(null);
        } else {
          result = null;
        }
        if (result == null || result.expression.isInstance(ErrorExpression.class)) {
          classOk = false;
        }

        if (newDef) {
          typedDef.implementField(entry.getKey(), new LamExpression(Sort.STD, parameter, result == null ? new ErrorExpression(null, null) : result.expression));
        }
        myVisitor.getContext().clear();
        myVisitor.getFreeBindings().clear();

        if (result != null) {
          if (newDef && entry.getKey() == lastField) {
            dfs.addDependencies(entry.getKey(), FieldsCollector.getFields(result.expression, parameter, typedDef.getFields()));
            dfs.setImplementedFields(implementedHere.keySet());
            for (ClassField field : typedDef.getFields()) {
              cycle = dfs.findCycle(field);
              if (cycle != null) {
                break;
              }
            }
          } else {
            cycle = dfs.checkDependencies(entry.getKey(), FieldsCollector.getFields(result.expression, parameter, typedDef.getFields()));
          }
          if (cycle != null) {
            myErrorReporter.report(CycleError.fromTypechecked(cycle, def));
            implementedHere.clear();
            break;
          }
        }
      }

      if (cycle == null) {
        typedDef.setTypecheckingFieldOrder(dfs.getFieldOrder());
      }
    }

    if (!alreadyImplementFields.isEmpty()) {
      myErrorReporter.report(new FieldsImplementationError(true, alreadyImplementFields, alreadyImplementFields.size() > 1 ? def : alreadyImplementedSourceNode));
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

      Set<ClassField> goodFields = new HashSet<>(typedDef.getPersonalFields());
      GoodThisParametersVisitor visitor = new GoodThisParametersVisitor(goodFields);
      for (ClassDefinition superClass : typedDef.getSuperClasses()) {
        goodFields.addAll(superClass.getGoodThisFields());
      }
      for (ClassField field : typedDef.getPersonalFields()) {
        field.getType(Sort.STD).getCodomain().accept(visitor, null);
      }
      for (Concrete.ClassFieldImpl implementation : def.getImplementations()) {
        ClassField field = myVisitor.referableToClassField(implementation.getImplementedField(), null);
        if (field != null) {
          LamExpression impl = typedDef.getImplementation(field);
          if (impl != null) {
            impl.getBody().accept(visitor, null);
          }
        }
      }
      typedDef.setGoodThisFields(visitor.getGoodFields());

      Set<ClassField> typeClassFields = new HashSet<>();
      for (Concrete.ClassField cField : def.getFields()) {
        if (cField.getData().isParameterField()) {
          Concrete.Expression resultType = cField.getResultType();
          if (resultType instanceof Concrete.PiExpression) {
            resultType = ((Concrete.PiExpression) resultType).getCodomain();
          }
          Concrete.ReferenceExpression refExpr = Concrete.getReferenceExpressionInType(resultType);
          if (refExpr != null && refExpr.getReferent() instanceof ClassReferable) {
            ClassField field = myVisitor.referableToClassField(cField.getData(), null);
            if (field != null) {
              typeClassFields.add(field);
            }
          }
        }
      }
      if (!typeClassFields.isEmpty()) {
        typedDef.setTypeClassFields(typeClassFields);
      }
    }
  }

  private void addName(Map<String, Referable> names, TCReferable data) {
    Referable prev = names.putIfAbsent(data.textRepresentation(), data);
    if (prev != null) {
      myErrorReporter.report(new DuplicateNameError(Error.Level.ERROR, data, prev));
    }
  }

  private static class DFS {
    private final ClassDefinition classDef;
    private final Map<ClassField, Boolean> state = new HashMap<>();
    private final Map<ClassField, Set<ClassField>> references = new HashMap<>();
    private Set<ClassField> implementedFields = null;
    private List<ClassField> fieldOrder = null;

    private DFS(ClassDefinition classDef) {
      this.classDef = classDef;
    }

    List<ClassField> findCycle(ClassField field) {
      List<ClassField> cycle = dfs(field);
      if (cycle != null) {
        Collections.reverse(cycle);
        for (ClassField dep : cycle) {
          references.remove(dep);
        }
      }
      return cycle;
    }

    private List<ClassField> dfs(ClassField field) {
      Boolean prevState = state.putIfAbsent(field, false);
      if (Boolean.TRUE.equals(prevState)) {
        return null;
      }
      if (Boolean.FALSE.equals(prevState)) {
        List<ClassField> cycle = new ArrayList<>();
        cycle.add(field);
        return cycle;
      }

      Set<ClassField> deps = references.computeIfAbsent(field, f -> {
        LamExpression impl = classDef.getImplementation(field);
        PiExpression type = field.getType(Sort.STD);
        Set<ClassField> result = FieldsCollector.getFields(type.getCodomain(), type.getParameters(), classDef.getFields());
        if (impl != null) {
          FieldsCollector.getFields(impl.getBody(), impl.getParameters(), classDef.getFields(), result);
        }
        return result;
      });

      for (ClassField dep : deps) {
        List<ClassField> cycle = dfs(dep);
        if (cycle != null) {
          if (cycle.get(0) != field) {
            cycle.add(field);
          }
          return cycle;
        }
      }

      state.put(field, true);
      if (fieldOrder != null && !classDef.isImplemented(field) && !implementedFields.contains(field)) {
        fieldOrder.add(field);
      }
      return null;
    }

    List<ClassField> checkDependencies(ClassField field, Collection<? extends ClassField> dependencies) {
      for (ClassField dependency : dependencies) {
        if (dependency == field) {
          return Collections.singletonList(field);
        }

        state.clear();
        state.put(field, false);
        List<ClassField> cycle = dfs(dependency);
        if (cycle != null) {
          Collections.reverse(cycle.subList(1, cycle.size()));
          return cycle;
        }
      }
      references.computeIfAbsent(field, f -> new HashSet<>()).addAll(dependencies);
      return null;
    }

    void addDependencies(ClassField field, Collection<? extends ClassField> dependencies) {
      state.clear();
      references.computeIfAbsent(field, f -> new HashSet<>()).addAll(dependencies);
    }

    List<ClassField> getFieldOrder() {
      return fieldOrder;
    }

    void setImplementedFields(Set<ClassField> implementedFields) {
      this.implementedFields = implementedFields;
      fieldOrder = new ArrayList<>();
    }
  }

  private ClassField typecheckClassField(Concrete.ClassField def, ClassDefinition parentClass, List<LocalInstance> localInstances, boolean newDef) {
    if (!def.getParameters().isEmpty()) {
      def.setResultType(new Concrete.PiExpression(def.getParameters().get(0).getData(), new ArrayList<>(def.getParameters()), def.getResultType()));
      def.getParameters().clear();
    }

    boolean isProperty;
    boolean ok;
    PiExpression piType;
    ClassField typedDef = null;
    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myVisitor.getFreeBindings())) {
      try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myVisitor.getContext())) {
        Concrete.Expression codomain;
        TypedSingleDependentLink thisParam = new TypedSingleDependentLink(false, "this", new ClassCallExpression(parentClass, Sort.STD), true);
        myVisitor.getFreeBindings().add(thisParam);
        if (def.getResultType() instanceof Concrete.PiExpression) {
          Concrete.PiExpression piExpr = (Concrete.PiExpression) def.getResultType();
          if (piExpr.getParameters().size() == 1) {
            codomain = piExpr.codomain;
          } else {
            codomain = new Concrete.PiExpression(piExpr.getParameters().get(1).getData(), piExpr.getParameters().subList(1, piExpr.getParameters().size()), piExpr.codomain);
          }
          myVisitor.getContext().put(piExpr.getParameters().get(0).getReferableList().get(0), thisParam);
        } else {
          myErrorReporter.report(new TypecheckingError("Internal error: class field must have a function type", def));
          codomain = def.getResultType();
        }

        setClassLocalInstancePool(localInstances, thisParam);
        isProperty = isPropLevel(codomain);
        Type typeResult = myVisitor.finalCheckType(codomain, def.getKind() == ClassFieldKind.PROPERTY && def.getResultTypeLevel() == null && !isProperty ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA);
        myInstancePool.setInstancePool(null);
        ok = typeResult != null;
        piType = new PiExpression(typeResult != null ? Sort.STD.max(typeResult.getSortOfType()) : Sort.STD, thisParam, typeResult != null ? typeResult.getExpr() : new ErrorExpression(null, null));

        if (def.getResultTypeLevel() != null && def.getKind() == ClassFieldKind.FIELD) {
          myErrorReporter.report(new TypecheckingError("\\level is allowed only for properties", def.getResultTypeLevel()));
          def.setResultTypeLevel(null);
        }
        if (newDef) {
          typedDef = addField(def.getData(), parentClass, piType, null);
        }

        if (def.getResultTypeLevel() != null && def.getResultType() instanceof Concrete.PiExpression) {
          List<Concrete.TypeParameter> parameters = ((Concrete.PiExpression) def.getResultType()).getParameters();
          Expression resultType = piType;
          SingleDependentLink link = EmptyDependentLink.getInstance();
          loop:
          for (Concrete.TypeParameter parameter : parameters) {
            for (Referable referable : parameter.getReferableList()) {
              if (!link.hasNext()) {
                if (!(resultType instanceof PiExpression)) {
                  resultType = null;
                  break loop;
                }
                link = ((PiExpression) resultType).getParameters();
                resultType = ((PiExpression) resultType).getCodomain();
              }
              if (referable != null) {
                myVisitor.getContext().put(referable, link);
              }
              myVisitor.getFreeBindings().add(link);
              link = link.getNext();
            }
          }
          if (!link.hasNext() && resultType != null) {
            typecheckResultTypeLevel(def.getResultTypeLevel(), false, true, resultType, null, typedDef, newDef);
          }
        }
      }
    }

    GoodThisParametersVisitor goodThisParametersVisitor = new GoodThisParametersVisitor(piType.getParameters());
    piType.getCodomain().accept(goodThisParametersVisitor, null);
    List<Boolean> goodThisParams = goodThisParametersVisitor.getGoodParameters();
    if (goodThisParams.isEmpty() || !goodThisParams.get(0)) {
      myErrorReporter.report(new TypecheckingError("The type of the field contains illegal \\this occurrence", def.getResultType()));
      ok = false;
      if (newDef) {
        typedDef.setType(new PiExpression(piType.getResultSort(), piType.getParameters(), new ErrorExpression(null, null)));
      }
    }

    if (!newDef) {
      return null;
    }

    if (def.getKind() == ClassFieldKind.FIELD) {
      isProperty = false;
    } else if (!isProperty) {
      if (typedDef.getTypeLevel() == null && def.getKind() == ClassFieldKind.ANY) {
        Expression universe = piType.getCodomain().getType();
        isProperty = universe instanceof UniverseExpression && ((UniverseExpression) universe).getSort().isProp();
      } else {
        isProperty = true;
      }
    }

    if (isProperty) {
      typedDef.setIsProperty();
    }
    if (!ok) {
      typedDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
    }
    return typedDef;
  }

  private void setClassLocalInstancePool(List<LocalInstance> localInstances, Binding thisParam) {
    if (!localInstances.isEmpty()) {
      LocalInstancePool localInstancePool = new LocalInstancePool(myVisitor);
      myInstancePool.setInstancePool(localInstancePool);
      for (LocalInstance localInstance : localInstances) {
        ClassField classifyingField = localInstance.classDefinition.getClassifyingField();
        Expression instance = FieldCallExpression.make(localInstance.instanceField, Sort.STD, new ReferenceExpression(thisParam));
        localInstancePool.addInstance(classifyingField == null ? null : FieldCallExpression.make(classifyingField, localInstance.instanceField.getType(Sort.STD).getSortOfType(), instance), localInstance.classReferable, instance, localInstance.concreteField);
      }
    }
  }

  private ClassField addField(TCFieldReferable fieldRef, ClassDefinition parentClass, PiExpression piType, Expression typeLevel) {
    ClassField typedDef = new ClassField(fieldRef, parentClass, piType, typeLevel);
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

  private ClassCallExpression typecheckCoClauses(FunctionDefinition typedDef, Concrete.Definition def, Concrete.Expression resultType, Concrete.Expression resultTypeLevel, List<Concrete.ClassFieldImpl> classFieldImpls, boolean newDef) {
    ClassCallExpression type;
    CheckTypeVisitor.Result result;
    Set<ClassField> pseudoImplemented;
    if (typedDef.isLemma()) {
      CheckTypeVisitor.Result typeResult = myVisitor.finalCheckExpr(resultType, resultTypeLevel == null ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA, false);
      if (typeResult == null || !(typeResult.expression instanceof ClassCallExpression)) {
        return null;
      }
      type = (ClassCallExpression) typeResult.expression;
      pseudoImplemented = new HashSet<>();
      result = myVisitor.visitClassExt(classFieldImpls, ExpectedType.OMEGA, type, pseudoImplemented, resultType);
    } else {
      pseudoImplemented = Collections.emptySet();
      result = myVisitor.finalCheckExpr(Concrete.ClassExtExpression.make(def.getData(), resultType, classFieldImpls), ExpectedType.OMEGA, false);
      if (result == null || !(result.expression instanceof ClassCallExpression)) {
        return null;
      }
      type = (ClassCallExpression) result.expression;
    }

    myVisitor.checkAllImplemented((ClassCallExpression) result.expression, pseudoImplemented, def);
    if (newDef) {
      typedDef.setResultType(type);
      typedDef.setStatus(myVisitor.getStatus());
    }
    return type;
  }

  private int compareExpressions(Expression expr1, Expression expr2) {
    if (expr2 instanceof ErrorExpression) {
      return 1;
    }
    expr1 = expr1.normalize(NormalizeVisitor.Mode.WHNF);

    while (expr2 instanceof LamExpression) {
      expr2 = ((LamExpression) expr2).getBody();
    }

    if (expr2 instanceof UniverseExpression) {
      return expr1 instanceof UniverseExpression && ((UniverseExpression) expr1).getSort().equals(((UniverseExpression) expr2).getSort()) ? 0 : 1;
    }

    if (expr2 instanceof IntegerExpression) {
      return expr1 instanceof IntegerExpression ? ((IntegerExpression) expr1).compare((IntegerExpression) expr2) : 1;
    }

    if (expr2 instanceof DataCallExpression) {
      int cmp = 0;
      if (expr1 instanceof DataCallExpression && ((DataCallExpression) expr1).getDefinition() == ((DataCallExpression) expr2).getDefinition()) {
        List<Expression> args1 = ((DataCallExpression) expr1).getDefCallArguments();
        List<Expression> args2 = ((DataCallExpression) expr2).getDefCallArguments();
        for (int i = 0; i < args1.size(); i++) {
          int argCmp = compareExpressions(args1.get(i), args2.get(i));
          if (argCmp == 1) {
            cmp = 1;
            break;
          }
          if (argCmp == -1) {
            cmp = -1;
          }
        }
        if (cmp == -1) {
          return -1;
        }
      }

      for (Expression arg : ((DataCallExpression) expr2).getDefCallArguments()) {
        if (compareExpressions(expr1, arg) != 1) {
          return -1;
        }
      }

      return cmp;
    }

    if (expr2 instanceof ClassCallExpression) {
      int cmp = 0;
      if (expr1 instanceof ClassCallExpression && ((ClassCallExpression) expr1).getDefinition() == ((ClassCallExpression) expr2).getDefinition() && ((ClassCallExpression) expr1).getImplementedHere().size() == ((ClassCallExpression) expr2).getImplementedHere().size()) {
        for (Map.Entry<ClassField, Expression> entry : ((ClassCallExpression) expr1).getImplementedHere().entrySet()) {
          Expression impl2 = ((ClassCallExpression) expr2).getImplementedHere().get(entry.getKey());
          if (impl2 == null) {
            cmp = 1;
            break;
          }

          int argCmp = compareExpressions(entry.getValue(), impl2);
          if (argCmp == 1) {
            cmp = 1;
            break;
          }
          if (argCmp == -1) {
            cmp = -1;
          }
        }
        if (cmp == -1) {
          return -1;
        }
      }

      for (Expression arg : ((ClassCallExpression) expr2).getImplementedHere().values()) {
        if (compareExpressions(expr1, arg) != 1) {
          return -1;
        }
      }

      return cmp;
    }

    return Expression.compare(expr1, expr2, Equations.CMP.EQ) ? 0 : 1;
  }
}
