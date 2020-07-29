package org.arend.typechecking.visitor;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.*;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.*;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.CountingErrorReporter;
import org.arend.error.IncorrectExpressionException;
import org.arend.ext.ArendExtension;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.typechecking.LevelProver;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.term.ClassFieldKind;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.concrete.FreeReferablesVisitor;
import org.arend.typechecking.FieldDFS;
import org.arend.typechecking.covariance.ParametersCovarianceChecker;
import org.arend.typechecking.covariance.RecursiveDataChecker;
import org.arend.typechecking.covariance.UniverseInParametersChecker;
import org.arend.typechecking.covariance.UniverseKindChecker;
import org.arend.typechecking.error.CycleError;
import org.arend.typechecking.error.ErrorReporterCounter;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.instance.pool.LocalInstancePool;
import org.arend.typechecking.patternmatching.ConditionsChecking;
import org.arend.typechecking.patternmatching.ElimTypechecking;
import org.arend.typechecking.patternmatching.ExtElimClause;
import org.arend.typechecking.patternmatching.PatternTypechecking;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.util.Pair;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;

public class DefinitionTypechecker extends BaseDefinitionTypechecker implements ConcreteDefinitionVisitor<Void, List<ExtElimClause>> {
  protected CheckTypeVisitor typechecker;
  private GlobalInstancePool myInstancePool;

  public DefinitionTypechecker(CheckTypeVisitor typechecker) {
    super(typechecker == null ? null : typechecker.errorReporter);
    this.typechecker = typechecker;
    myInstancePool = typechecker == null ? null : typechecker.getInstancePool();
  }

  public void setTypechecker(CheckTypeVisitor typechecker) {
    this.typechecker = typechecker;
    this.errorReporter = typechecker.errorReporter;
    myInstancePool = typechecker.getInstancePool();
  }

  public Definition typecheckHeader(Definition typechecked, GlobalInstancePool instancePool, Concrete.Definition definition) {
    LocalInstancePool localInstancePool = new LocalInstancePool(typechecker);
    instancePool.setInstancePool(localInstancePool);
    typechecker.setInstancePool(instancePool);

    if (definition instanceof Concrete.BaseFunctionDefinition) {
      FunctionDefinition functionDef = typechecked != null ? (FunctionDefinition) typechecked : ((Concrete.BaseFunctionDefinition) definition).getKind() == FunctionKind.CONS ? new DConstructor(definition.getData()) : new FunctionDefinition(definition.getData());
      try {
        typecheckFunctionHeader(functionDef, (Concrete.BaseFunctionDefinition) definition, localInstancePool, typechecked == null || !typechecked.status().headerIsOK());
      } catch (IncorrectExpressionException e) {
        errorReporter.report(new TypecheckingError(e.getMessage(), definition));
      }
      return functionDef;
    } else
    if (definition instanceof Concrete.DataDefinition) {
      DataDefinition dataDef = typechecked != null ? (DataDefinition) typechecked : new DataDefinition(definition.getData());
      try {
        typecheckDataHeader(dataDef, (Concrete.DataDefinition) definition, localInstancePool, typechecked == null || !typechecked.status().headerIsOK());
      } catch (IncorrectExpressionException e) {
        errorReporter.report(new TypecheckingError(e.getMessage(), definition));
      }
      if (dataDef.getSort() == null || dataDef.getSort().getPLevel().isInfinity()) {
        errorReporter.report(new TypecheckingError("Cannot infer the sort of a recursive data type", definition));
        if (typechecked == null) {
          dataDef.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          dataDef.setSort(Sort.SET0);
        }
      }
      return dataDef;
    } else {
      throw new IllegalStateException();
    }
  }

  public List<ExtElimClause> typecheckBody(Definition definition, Concrete.Definition def, Set<DataDefinition> dataDefinitions, boolean newDef) {
    if (definition instanceof FunctionDefinition) {
      try {
        return typecheckFunctionBody((FunctionDefinition) definition, (Concrete.BaseFunctionDefinition) def, newDef);
      } catch (IncorrectExpressionException e) {
        errorReporter.report(new TypecheckingError(e.getMessage(), def));
      }
    } else
    if (definition instanceof DataDefinition) {
      try {
        if (!typecheckDataBody((DataDefinition) definition, (Concrete.DataDefinition) def, false, dataDefinitions, newDef) && newDef) {
          definition.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
      } catch (IncorrectExpressionException e) {
        errorReporter.report(new TypecheckingError(e.getMessage(), def));
      }
    } else {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public List<ExtElimClause> visitMeta(Concrete.MetaDefinition def, Void params) {
    Definition typechecked = def.getData().getTypechecked();
    var localInstancePool = new LocalInstancePool(typechecker);
    myInstancePool.setInstancePool(localInstancePool);
    typechecker.setInstancePool(myInstancePool);

    // TODO: produce a meta definition here
    return null;
  }

  @Override
  public List<ExtElimClause> visitFunction(Concrete.BaseFunctionDefinition def, Void params) {
    Definition typechecked = def.getData().getTypechecked();
    LocalInstancePool localInstancePool = new LocalInstancePool(typechecker);
    myInstancePool.setInstancePool(localInstancePool);
    typechecker.setInstancePool(myInstancePool);

    FunctionDefinition definition = typechecked != null ? (FunctionDefinition) typechecked : def.getKind() == FunctionKind.CONS ? new DConstructor(def.getData()) : new FunctionDefinition(def.getData());
    try {
      typecheckFunctionHeader(definition, def, localInstancePool, typechecked == null || !typechecked.status().headerIsOK());
      return typecheckFunctionBody(definition, def, typechecked == null);
    } catch (IncorrectExpressionException e) {
      errorReporter.report(new TypecheckingError(e.getMessage(), def));
      return null;
    }
  }

  @Override
  public List<ExtElimClause> visitData(Concrete.DataDefinition def, Void params) {
    Definition typechecked = def.getData().getTypechecked();
    LocalInstancePool localInstancePool = new LocalInstancePool(typechecker);
    myInstancePool.setInstancePool(localInstancePool);
    typechecker.setInstancePool(myInstancePool);

    DataDefinition definition = typechecked != null ? (DataDefinition) typechecked : new DataDefinition(def.getData());
    try {
      typecheckDataHeader(definition, def, localInstancePool, typechecked == null || !typechecked.status().headerIsOK());
      if (definition.status().headerIsOK()) {
        typecheckDataBody(definition, def, true, Collections.singleton(definition), typechecked == null);
      }
    } catch (IncorrectExpressionException e) {
      errorReporter.report(new TypecheckingError(e.getMessage(), def));
      return null;
    }
    return null;
  }

  @Override
  public List<ExtElimClause> visitClass(Concrete.ClassDefinition def, Void params) {
    Definition typechecked = def.getData().getTypechecked();
    typechecker.setStatus(def.getStatus().getTypecheckingStatus());

    if (def.isRecursive()) {
      errorReporter.report(new TypecheckingError("A class cannot be recursive", def));
      if (typechecked != null) {
        return null;
      }
    }

    ClassDefinition definition = typechecked != null ? (ClassDefinition) typechecked : new ClassDefinition(def.getData());
    if (typechecked == null) {
      def.getData().setTypecheckedIfAbsent(definition);
    }
    if (def.isRecursive()) {
      definition.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);

      for (Concrete.ClassElement element : def.getElements()) {
        if (element instanceof Concrete.ClassField) {
          addField(((Concrete.ClassField) element).getData(), definition, new PiExpression(Sort.STD, new TypedSingleDependentLink(false, "this", new ClassCallExpression(definition, Sort.STD), true), new ErrorExpression()), null).setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
      }
    } else {
      try {
        typecheckClass(def, definition, typechecked == null || !typechecked.status().headerIsOK());
      } catch (IncorrectExpressionException e) {
        errorReporter.report(new TypecheckingError(e.getMessage(), def));
      }
    }
    return null;
  }

  private void getFreeVariablesClosure(Expression expression, Set<Binding> freeVars) {
    for (Binding var : FreeVariablesCollector.getFreeVariables(expression)) {
      if (freeVars.add(var)) {
        getFreeVariablesClosure(var.getTypeExpr(), freeVars);
      }
    }
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
          DependentLink link1 = definition instanceof FunctionDefinition ? link : link.getNext(); // if link1 == link, check the result type
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

            boolean checkResultType = link1 == link && definition instanceof FunctionDefinition;
            DependentLink actualLink = checkResultType ? EmptyDependentLink.getInstance() : link1;
            FreeVariablesClassifier.Result result;
            if (checkResultType) {
              Expression type = ((FunctionDefinition) definition).getResultType();
              result = type instanceof ReferenceExpression && ((ReferenceExpression) type).getBinding() == link ? FreeVariablesClassifier.Result.BAD : type.accept(classifier, true);
            } else {
              result = classifier.checkBinding(link1);
            }
            if ((result == FreeVariablesClassifier.Result.GOOD || result == FreeVariablesClassifier.Result.BOTH) && processed.contains(actualLink)) {
              found = true;
              processed.add(link);
              break;
            }
            if (result == FreeVariablesClassifier.Result.GOOD && (checkResultType || link1.isExplicit())) {
              found = true;
              processed.add(link);
              Set<Binding> freeVars = new HashSet<>();
              getFreeVariablesClosure(checkResultType ? ((FunctionDefinition) definition).getResultType() : link1.getTypeExpr(), freeVars);
              for (DependentLink link2 : parametersList) {
                for (; link2.hasNext() && link2 != actualLink; link2 = link2.getNext()) {
                  if (freeVars.contains(link2)) {
                    processed.add(link2);
                  }
                }
                if (!checkResultType && link2 == link1) {
                  break;
                }
              }
              processed.add(actualLink);
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
      if (link.hasNext()) {
        link = link.getNext();
        if (!link.hasNext() && isDataTypeParameter) {
          link = parametersList.get(1);
          isDataTypeParameter = false;
        }
      }
    }

    if (needReorder) {
      Map<Binding,Integer> map = new HashMap<>();
      if (definition instanceof FunctionDefinition) {
        map.put(EmptyDependentLink.getInstance(), -1);
      }
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
      if (order.get(order.size() - 1) == -1) {
        order.remove(order.size() - 1);
      }

      definition.setParametersTypecheckingOrder(order);
    }
  }

  private boolean checkForUniverses(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      if (new UniverseInParametersChecker().check(link.getTypeExpr())) {
        return true;
      }
    }
    return false;
  }

  private Integer checkResultTypeLevel(TypecheckingResult result, boolean isLemma, boolean isProperty, Expression resultType, FunctionDefinition funDef, ClassField classField, boolean newDef, Concrete.SourceNode sourceNode) {
    if (result == null || resultType == null) {
      return null;
    }

    Integer level = typechecker.getExpressionLevel(EmptyDependentLink.getInstance(), result.type, resultType, DummyEquations.getInstance(), sourceNode);
    if (level != null) {
      if (!checkLevel(isLemma, isProperty, level, null, sourceNode)) {
        if (newDef && funDef != null) {
          funDef.setKind(CoreFunctionDefinition.Kind.FUNC);
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

  private Integer typecheckResultTypeLevel(Concrete.Expression resultTypeLevel, boolean isLemma, boolean isProperty, Expression resultType, FunctionDefinition funDef, ClassField classField, boolean newDef) {
    return resultTypeLevel == null ? null : checkResultTypeLevel(typechecker.finalCheckExpr(resultTypeLevel, null), isLemma, isProperty, resultType, funDef, classField, newDef, resultTypeLevel);
  }

  private void calculateGoodThisParameters(Constructor definition) {
    GoodThisParametersVisitor visitor;
    if (definition.getPatterns() == null) {
      visitor = new GoodThisParametersVisitor(definition.getParameters());
    } else {
      visitor = new GoodThisParametersVisitor(Pattern.getFirstBinding(definition.getPatterns()));
      visitor.visitParameters(definition.getParameters(), null);
    }
    visitor.visitBody(definition.getBody(), null);
    definition.setGoodThisParameters(visitor.getGoodParameters());
  }

  private void calculateTypeClassParameters(Concrete.ReferableDefinition refDef, Definition def) {
    List<Definition.TypeClassParameterKind> typeClassParameters = new ArrayList<>();

    if (def instanceof Constructor) {
      Constructor constructor = (Constructor) def;
      List<Definition.TypeClassParameterKind> dataTypeParameters = constructor.getDataType().getTypeClassParameters();
      if (dataTypeParameters.isEmpty()) {
        for (DependentLink link = constructor.getDataTypeParameters(); link.hasNext(); link = link.getNext()) {
          typeClassParameters.add(Definition.TypeClassParameterKind.NO);
        }
      } else {
        List<ExpressionPattern> patterns = constructor.getPatterns();
        if (patterns == null) {
          typeClassParameters.addAll(dataTypeParameters);
        } else {
          assert patterns.size() == dataTypeParameters.size();
          int i = 0;
          for (ExpressionPattern pattern : patterns) {
            if (pattern instanceof BindingPattern) {
              typeClassParameters.add(dataTypeParameters.get(i));
            } else {
              DependentLink next = i + 1 < patterns.size() ? patterns.get(i + 1).getFirstBinding() : EmptyDependentLink.getInstance();
              for (DependentLink link = pattern.getFirstBinding(); link.hasNext() && link != next; link = link.getNext()) {
                typeClassParameters.add(Definition.TypeClassParameterKind.NO);
              }
            }
            i++;
          }
        }
      }
    }

    for (Concrete.Parameter parameter : Objects.requireNonNull(Concrete.getParameters(refDef, true))) {
      boolean isTypeClass = isTypeClassRef(parameter.getType(), false);
      for (int i = 0; i < parameter.getNumberOfParameters(); i++) {
        typeClassParameters.add(isTypeClass ? Definition.TypeClassParameterKind.YES : Definition.TypeClassParameterKind.NO);
      }
    }

    for (Definition.TypeClassParameterKind kind : typeClassParameters) {
      if (kind != Definition.TypeClassParameterKind.NO) {
        def.setTypeClassParameters(typeClassParameters);
        return;
      }
    }
  }

  private boolean isTypeClassRef(Concrete.Expression expr, boolean allowRecord) {
    if (expr == null) {
      return false;
    }
    Referable typeRef = expr.getUnderlyingReferable();
    Definition typeDef = typeRef instanceof TCReferable ? ((TCReferable) typeRef).getTypechecked() : null;
    return typeDef instanceof ClassDefinition && (allowRecord || !((ClassDefinition) typeDef).isRecord());
  }

  private Pair<Sort,Expression> typecheckParameters(Concrete.ReferableDefinition def, LinkList list, LocalInstancePool localInstancePool, Sort expectedSort, DependentLink oldParameters, PiExpression fieldType) {
    Sort sort = Sort.PROP;

    if (oldParameters != null) {
      list.append(oldParameters);
      fieldType = null;
    }

    DependentLink thisRef = fieldType == null ? null : fieldType.getParameters();
    Expression resultType = fieldType == null ? null : fieldType.getCodomain();
    ExprSubstitution substitution = fieldType == null ? null : new ExprSubstitution();

    boolean first = true;
    for (Concrete.Parameter parameter : Objects.requireNonNull(Concrete.getParameters(def, true))) {
      if (resultType != null && !(resultType instanceof ErrorExpression)) {
        resultType = resultType.normalize(NormalizationMode.WHNF).getUnderlyingExpression();
      }

      Type paramResult = null;
      if (parameter.getType() != null) {
        paramResult = typechecker.finalCheckType(parameter.getType(), expectedSort == null ? Type.OMEGA : new UniverseExpression(expectedSort), false);
      } else {
        if (resultType instanceof PiExpression) {
          SingleDependentLink param = ((PiExpression) resultType).getParameters();
          if (param.isExplicit() != parameter.isExplicit()) {
            errorReporter.report(new ArgumentExplicitnessError(param.isExplicit(), parameter));
            break;
          }
          Type paramType = param.getType();
          if (paramType.getExpr().findBinding(thisRef)) {
            errorReporter.report(new TypeFromFieldError(TypeFromFieldError.parameter(), paramType.getExpr(), parameter));
          } else {
            paramResult = paramType.subst(new SubstVisitor(substitution, LevelSubstitution.EMPTY));
          }
        } else if (resultType == null || !resultType.isError()) {
          if (resultType == null) {
            errorReporter.report(new TypecheckingError("Expected a typed parameter", parameter));
          } else {
            errorReporter.report(new FieldTypeParameterError(fieldType.getCodomain(), parameter));
            resultType = new ErrorExpression();
          }
        }
      }
      if (paramResult == null) {
        paramResult = new TypeExpression(new ErrorExpression(), Sort.SET0);
      }
      sort = sort.max(paramResult.getSortOfType());

      DependentLink param;
      int numberOfParameters;
      boolean oldParametersOK = true;
      if (parameter instanceof Concrete.TelescopeParameter) {
        List<? extends Referable> referableList = parameter.getReferableList();
        if (referableList.isEmpty()) {
          errorReporter.report(new TypecheckingError("Empty parameter list", parameter));
          continue;
        }

        List<String> names = parameter.getNames();
        param = oldParameters != null
          ? oldParameters
          : referableList.size() == 1 && referableList.get(0) instanceof HiddenLocalReferable
            ? new TypedDependentLink(parameter.isExplicit(), names.get(0), paramResult, true, EmptyDependentLink.getInstance())
            : parameter(parameter.isExplicit(), names, paramResult);
        numberOfParameters = names.size();

        if (oldParameters == null) {
          int i = 0;
          for (DependentLink link = param; link.hasNext(); link = link.getNext(), i++) {
            typechecker.addBinding(referableList.get(i), link);
          }
        } else {
          for (int i = 0; i < names.size(); i++) {
            if (oldParameters.hasNext()) {
              typechecker.addBinding(referableList.get(i), oldParameters);
              oldParameters = oldParameters.getNext();
            } else {
              oldParametersOK = false;
              break;
            }
          }
        }
      } else {
        numberOfParameters = 1;
        if (oldParameters != null) {
          param = oldParameters;
          if (oldParameters.hasNext()) {
            typechecker.addBinding(parameter instanceof Concrete.NameParameter ? ((Concrete.NameParameter) parameter).getReferable() : null, oldParameters);
            oldParameters = oldParameters.getNext();
          } else {
            oldParametersOK = false;
          }
        } else {
          Referable ref = parameter.getReferableList().get(0);
          param = parameter(parameter.isExplicit(), ref == null ? null : ref.getRefName(), paramResult);
          typechecker.addBinding(ref, param);
        }
      }
      if (!oldParametersOK) {
        errorReporter.report(new TypecheckingError("Cannot typecheck definition. Try to clear cache", parameter));
        return null;
      }

      for (DependentLink link = param; link.hasNext(); link = link.getNext()) {
        if (!(resultType instanceof PiExpression)) {
          if (resultType != null && !resultType.isError()) {
            errorReporter.report(new FieldTypeParameterError(fieldType, parameter));
            resultType = new ErrorExpression();
          }
          break;
        }

        PiExpression piExpr = (PiExpression) resultType;
        substitution.add(piExpr.getParameters(), new ReferenceExpression(param));
        if (piExpr.getParameters().getNext().hasNext()) {
          resultType = new PiExpression(piExpr.getResultSort(), piExpr.getParameters().getNext(), piExpr.getCodomain());
        } else {
          resultType = piExpr.getCodomain().normalize(NormalizationMode.WHNF).getUnderlyingExpression();
        }
      }

      if (localInstancePool != null && paramResult instanceof ClassCallExpression) {
        ClassDefinition classDef = ((ClassCallExpression) paramResult).getDefinition();
        if (!classDef.isRecord()) {
          ClassField classifyingField = classDef.getClassifyingField();
          int i = 0;
          for (DependentLink link = param; i < numberOfParameters; link = link.getNext(), i++) {
            ReferenceExpression reference = new ReferenceExpression(link);
            if (classifyingField == null) {
              localInstancePool.addLocalInstance(null, classDef, reference);
            } else {
              localInstancePool.addLocalInstance(FieldCallExpression.make(classifyingField, ((ClassCallExpression) paramResult).getSortArgument(), reference), classDef, reference);
            }
          }
        }
      }

      if (first && localInstancePool != null && def instanceof Concrete.Definition && ((Concrete.Definition) def).enclosingClass != null) {
        addEnclosingClassInstances((Concrete.Definition) def, param, localInstancePool);
      }

      if (oldParameters == null) {
        list.append(param);
      }

      first = false;
    }

    return new Pair<>(sort, resultType == null ? null : resultType.subst(substitution));
  }

  private boolean checkLevel(boolean isLemma, boolean isProperty, Integer level, Sort actualSort, Concrete.SourceNode sourceNode) {
    if ((isLemma || isProperty) && (level == null || level != -1)) {
      Sort sort = level != null ? new Sort(new Level(LevelVariable.PVAR), new Level(actualSort == null || !actualSort.getHLevel().isClosed() ? level : Math.min(level, actualSort.getHLevel().getConstant()))) : actualSort;
      errorReporter.report(new LevelMismatchError(isLemma, sort, sourceNode));
      return false;
    } else {
      return true;
    }
  }

  private static boolean checkResultTypeLater(Concrete.BaseFunctionDefinition def) {
    return def.getBody() instanceof Concrete.TermFunctionBody && !def.isRecursive() && def.getKind() != FunctionKind.LEVEL && (!(def.getBody().getTerm() instanceof Concrete.CaseExpression) || ((Concrete.CaseExpression) def.getBody().getTerm()).getResultType() != null);
  }

  private void addEnclosingClassInstances(Concrete.Definition def, Binding thisParam, LocalInstancePool localInstancePool) {
    Definition classDef = def.enclosingClass.getTypechecked();
    if (!(classDef instanceof ClassDefinition)) {
      return;
    }

    List<LocalInstance> instances = new ArrayList<>();
    for (ClassField field : ((ClassDefinition) classDef).getPersonalFields()) {
      if (field.getReferable().isParameterField() && field.getResultType() instanceof ClassCallExpression) {
        ClassCallExpression classCall = (ClassCallExpression) field.getResultType();
        if (!classCall.getDefinition().isRecord()) {
          instances.add(new LocalInstance(classCall, field));
        }
      }
    }

    addLocalInstances(instances, thisParam, null, localInstancePool);
  }

  @SuppressWarnings("UnusedReturnValue")
  private boolean typecheckFunctionHeader(FunctionDefinition typedDef, Concrete.BaseFunctionDefinition def, LocalInstancePool localInstancePool, boolean newDef) {
    if (def.enclosingClass != null) {
      typedDef.setHasEnclosingClass(true);
    }
    ClassField implementedField = def instanceof Concrete.CoClauseFunctionDefinition ? typechecker.referableToClassField(((Concrete.CoClauseFunctionDefinition) def).getImplementedField(), def) : null;
    FunctionKind kind = implementedField == null ? def.getKind() : implementedField.isProperty() && implementedField.getTypeLevel() == null ? FunctionKind.LEMMA : FunctionKind.FUNC;
    checkFunctionLevel(def, kind);

    LinkList list = new LinkList();
    Pair<Sort, Expression> pair = typecheckParameters(def, list, localInstancePool, null, newDef ? null : typedDef.getParameters(), implementedField == null ? null : implementedField.getType(Sort.STD));

    Expression expectedType = null;
    Concrete.Expression cResultType = def.getResultType();
    if (cResultType != null) {
      Type expectedTypeResult = pair == null
        ? new ErrorExpression()
        : def.getBody() instanceof Concrete.CoelimFunctionBody && !def.isRecursive()
          ? null // The result type will be typechecked together with all field implementations during body typechecking.
          : checkResultTypeLater(def)
            ? typechecker.checkType(cResultType, Type.OMEGA)
            : typechecker.finalCheckType(cResultType, Type.OMEGA, kind == FunctionKind.LEMMA && def.getResultTypeLevel() == null);
      if (expectedTypeResult != null) {
        expectedType = expectedTypeResult.getExpr();
      }
    }

    if (newDef) {
      if (pair != null && pair.proj2 != null && cResultType == null && implementedField != null) {
        expectedType = pair.proj2;
        if (expectedType.findBinding(implementedField.getType(Sort.STD).getParameters())) {
          errorReporter.report(new TypeFromFieldError(TypeFromFieldError.resultType(), expectedType, def));
          expectedType = null;
        }
      }
      if (expectedType == null) {
        expectedType = new ErrorExpression();
      }

      typedDef.setParameters(list.getFirst());
      typedDef.setResultType(expectedType);
      typedDef.setStatus(Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING);
      typedDef.setKind(kind.isSFunc() ? (kind == FunctionKind.LEMMA ? CoreFunctionDefinition.Kind.LEMMA : CoreFunctionDefinition.Kind.SFUNC) : kind == FunctionKind.INSTANCE ? CoreFunctionDefinition.Kind.INSTANCE : CoreFunctionDefinition.Kind.FUNC);
    }

    if (newDef) {
      def.getData().setTypecheckedIfAbsent(typedDef);
      calculateTypeClassParameters(def, typedDef);
      calculateParametersTypecheckingOrder(typedDef);
    }

    return pair != null;
  }

  // Returns a pair consisting of classCalls corresponding to the body and the type (so, the first one is an extension of the second).
  private Pair<ClassCallExpression,ClassCallExpression> typecheckCoClauses(FunctionDefinition typedDef, Concrete.BaseFunctionDefinition def, FunctionKind kind, List<Concrete.CoClauseElement> elements) {
    List<Concrete.ClassFieldImpl> classFieldImpls = new ArrayList<>(elements.size());
    for (Concrete.CoClauseElement element : elements) {
      if (element instanceof Concrete.ClassFieldImpl) {
        classFieldImpls.add((Concrete.ClassFieldImpl) element);
      } else if (element instanceof Concrete.CoClauseFunctionReference) {
        TCReferable ref = ((Concrete.CoClauseFunctionReference) element).getFunctionReference();
        classFieldImpls.add(new Concrete.ClassFieldImpl(element.getData(), element.getImplementedField(), new Concrete.ReferenceExpression(ref.getData(), ref), Collections.emptyList()));
      } else {
        throw new IllegalStateException();
      }
    }

    ClassCallExpression type;
    TypecheckingResult result;
    Set<ClassField> pseudoImplemented;
    Concrete.Expression resultType = def.getResultType();
    if (typedDef.isSFunc() || kind == FunctionKind.CONS) {
      TypecheckingResult typeResult = typechecker.finalCheckExpr(resultType, Type.OMEGA);
      if (typeResult == null || !(typeResult.expression instanceof ClassCallExpression)) {
        return null;
      }
      type = (ClassCallExpression) typeResult.expression;
      pseudoImplemented = new HashSet<>();
      result = typechecker.finalize(typechecker.typecheckClassExt(classFieldImpls, Type.OMEGA, type, pseudoImplemented, resultType), def, false);
      if (result == null || !(result.expression instanceof ClassCallExpression)) {
        return null;
      }
    } else {
      pseudoImplemented = Collections.emptySet();
      result = typechecker.finalCheckExpr(Concrete.ClassExtExpression.make(def.getData(), typechecker.desugarClassApp(resultType, true), classFieldImpls), Type.OMEGA);
      if (result == null || !(result.expression instanceof ClassCallExpression)) {
        return null;
      }
      type = (ClassCallExpression) result.expression;
    }

    typechecker.checkAllImplemented((ClassCallExpression) result.expression, pseudoImplemented, def);
    return new Pair<>((ClassCallExpression) result.expression, type);
  }

  private ExpressionPattern checkDConstructor(Expression expr, Set<DependentLink> usedVars, Concrete.SourceNode sourceNode) {
    if (expr instanceof ReferenceExpression && ((ReferenceExpression) expr).getBinding() instanceof DependentLink) {
      DependentLink var = (DependentLink) ((ReferenceExpression) expr).getBinding();
      if (!usedVars.add(var)) {
        errorReporter.report(new TypecheckingError("Variable '" + var.getName() + "' occurs multiple times in the body of \\cons", sourceNode));
        return null;
      }
      return new BindingPattern(var);
    }

    if (expr instanceof IntegerExpression) {
      int n;
      try {
        n = checkNumberInPattern(((IntegerExpression) expr).getSmallInteger(), errorReporter, sourceNode);
      } catch (ArithmeticException e) {
        n = Concrete.NumberPattern.MAX_VALUE;
      }
      ExpressionPattern pattern = new ConstructorExpressionPattern(new ConCallExpression(Prelude.ZERO, Sort.PROP, Collections.emptyList(), Collections.emptyList()), Collections.emptyList());
      for (int i = 0; i < n; i++) {
        pattern = new ConstructorExpressionPattern(new ConCallExpression(Prelude.SUC, Sort.PROP, Collections.emptyList(), Collections.emptyList()), Collections.singletonList(pattern));
      }
      return pattern;
    }

    if (!(expr instanceof ConCallExpression || expr instanceof FunCallExpression && ((FunCallExpression) expr).getDefinition() instanceof DConstructor || expr instanceof TupleExpression)) {
      if (!(expr instanceof ErrorExpression)) {
        errorReporter.report(new TypecheckingError("\\cons must contain only constructors and variables", sourceNode));
      }
      return null;
    }

    List<ExpressionPattern> patterns = new ArrayList<>();
    List<? extends Expression> arguments = expr instanceof DefCallExpression ? ((DefCallExpression) expr).getConCallArguments() : ((TupleExpression) expr).getFields();
    for (Expression argument : arguments) {
      ExpressionPattern pattern = checkDConstructor(argument, usedVars, sourceNode);
      if (pattern == null) {
        return null;
      }
      patterns.add(pattern);
    }

    if (expr instanceof ConCallExpression) {
      ConCallExpression conCall = (ConCallExpression) expr;
      return new ConstructorExpressionPattern(new ConCallExpression(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), Collections.emptyList()), patterns);
    }

    if (expr instanceof TupleExpression) {
      return new ConstructorExpressionPattern(((TupleExpression) expr).getSigmaType(), patterns);
    }

    DConstructor constructor = (DConstructor) ((FunCallExpression) expr).getDefinition();
    ExpressionPattern pattern = constructor.getPattern();
    if (pattern == null) {
      return null;
    }

    Map<DependentLink, ExpressionPattern> patternSubst = new HashMap<>();
    DependentLink link = DependentLink.Helper.get(constructor.getParameters(), constructor.getNumberOfParameters());
    for (ExpressionPattern patternArg : patterns) {
      patternSubst.put(link, patternArg);
      link = link.getNext();
    }
    DefCallExpression defCall = (DefCallExpression) expr;
    return pattern.subst(new ExprSubstitution().add(constructor.getParameters(), defCall.getDefCallArguments().subList(0, constructor.getNumberOfParameters())), defCall.getSortArgument().toLevelSubstitution(), patternSubst);
  }

  private ExpressionPattern checkDConstructor(ClassCallExpression type, NewExpression expr, Set<DependentLink> usedVars, Concrete.SourceNode sourceNode) {
    List<ExpressionPattern> patterns = new ArrayList<>();
    for (ClassField field : type.getDefinition().getFields()) {
      if (!type.isImplemented(field)) {
        ExpressionPattern pattern = checkDConstructor(expr.getImplementation(field), usedVars, sourceNode);
        if (pattern == null) {
          return null;
        }
        patterns.add(pattern);
      }
    }
    return new ConstructorExpressionPattern(type, patterns);
  }

  private Integer checkTypeLevel(Concrete.BaseFunctionDefinition def, FunctionDefinition typedDef, boolean newDef) {
    Expression type = typedDef.getResultType();
    Integer resultTypeLevel = type.isError() ? null : typecheckResultTypeLevel(def.getResultTypeLevel(), def.getKind() == FunctionKind.LEMMA, false, type, typedDef, null, newDef);
    if (resultTypeLevel == null && !type.isError()) {
      DefCallExpression defCall = type.cast(DefCallExpression.class);
      resultTypeLevel = defCall == null ? null : defCall.getUseLevel();
      if (resultTypeLevel == null) {
        defCall = type.getPiParameters(null, false).cast(DefCallExpression.class);
        if (defCall != null) {
          resultTypeLevel = defCall.getUseLevel();
        }
      }
    }

    if (def.getKind() == FunctionKind.LEMMA && resultTypeLevel == null && !type.isError()) {
      Sort sort = type.getSortOfType();
      if (sort == null || !sort.isProp()) {
        DefCallExpression defCall = type.cast(DefCallExpression.class);
        Integer level = defCall == null ? null : defCall.getUseLevel();
        Concrete.SourceNode sourceNode = def.getResultType() != null ? def.getResultType() : def;
        if ((level == null || level != -1) && sort != null && typechecker.getExtension() != null) {
          LevelProver prover = typechecker.getExtension().getLevelProver();
          if (prover != null) {
            TypecheckingResult result = typechecker.finalize(TypecheckingResult.fromChecked(prover.prove(null, type, CheckTypeVisitor.getLevelExpression(new TypeExpression(type, sort), -1), -1, sourceNode, typechecker)), sourceNode, false);
            if (result != null) {
              Integer level2 = checkResultTypeLevel(result, true, false, type, typedDef, null, newDef, def);
              return level != null && level2 != null ? Integer.valueOf(Math.min(level, level2)) : level != null ? level : level2;
            }
          }
        }
        if (!checkLevel(true, false, level, sort, sourceNode)) {
          if (newDef) {
            typedDef.setKind(CoreFunctionDefinition.Kind.SFUNC);
          }
        }
      }
    }

    return resultTypeLevel;
  }

  private List<ExtElimClause> typecheckFunctionBody(FunctionDefinition typedDef, Concrete.BaseFunctionDefinition def, boolean newDef) {
    FunctionKind kind = def.getKind();
    if (def instanceof Concrete.CoClauseFunctionDefinition) {
      Referable ref = ((Concrete.CoClauseFunctionDefinition) def).getImplementedField();
      if (ref instanceof TCReferable) {
        Definition fieldDef = ((TCReferable) ref).getTypechecked();
        if (fieldDef instanceof ClassField && DependentLink.Helper.size(typedDef.getParameters()) != Concrete.getNumberOfParameters(def.getParameters())) {
          if (newDef) {
            typechecker.setStatus(def.getStatus().getTypecheckingStatus());
            typedDef.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
            def.setTypechecked();
          }
          return null;
        }

        if (fieldDef instanceof ClassField && ((ClassField) fieldDef).isProperty()) {
          if (((ClassField) fieldDef).getTypeLevel() == null) {
            kind = FunctionKind.LEMMA;
          } else if (def.getResultType() == null) {
            boolean ok = true;
            for (Concrete.Parameter parameter : def.getParameters()) {
              if (parameter.getType() != null) {
                ok = false;
              }
            }
            if (ok) {
              kind = FunctionKind.LEMMA;
            }
          }
        }
      }
      if (kind != FunctionKind.LEMMA) {
        kind = FunctionKind.FUNC;
      }
    }

    if (typedDef.getResultType() == null) {
      typedDef.setResultType(new ErrorExpression());
    }
    Expression expectedType = typedDef.getResultType();

    GoodThisParametersVisitor goodThisParametersVisitor;
    if (newDef) {
      goodThisParametersVisitor = new GoodThisParametersVisitor(typedDef.getParameters());
      expectedType.accept(goodThisParametersVisitor, null);
      if (typedDef.getResultTypeLevel() != null) {
        typedDef.getResultTypeLevel().accept(goodThisParametersVisitor, null);
      }
      typedDef.setGoodThisParameters(goodThisParametersVisitor.getGoodParameters());
    } else {
      goodThisParametersVisitor = null;
    }

    List<ExtElimClause> clauses = null;
    Concrete.FunctionBody body = def.getBody();
    boolean checkLevelNow = (body instanceof Concrete.ElimFunctionBody || body.getTerm() instanceof Concrete.CaseExpression && def.getKind() != FunctionKind.LEVEL) && !checkResultTypeLater(def);
    Integer typeLevel = checkLevelNow ? checkTypeLevel(def, typedDef, newDef) : null;
    if (typeLevel != null) {
      if (body instanceof Concrete.ElimFunctionBody) {
        for (Concrete.FunctionClause clause : body.getClauses()) {
          if (clause.getExpression() instanceof Concrete.CaseExpression) {
            Concrete.CaseExpression caseExpr = (Concrete.CaseExpression) clause.getExpression();
            caseExpr.level = typeLevel;
            caseExpr.setSCase(true);
          }
        }
      } else {
        Concrete.CaseExpression caseExpr = (Concrete.CaseExpression) body.getTerm();
        caseExpr.level = typeLevel;
        caseExpr.setSCase(true);
      }
    }

    boolean bodyIsOK = false;
    ClassCallExpression consType = null;
    if (def.getKind() == FunctionKind.LEVEL && typedDef.getResultType() instanceof UniverseExpression && ((UniverseExpression) typedDef.getResultType()).getSort().getHLevel().isClosed() && (body instanceof Concrete.TermFunctionBody || body instanceof Concrete.ElimFunctionBody && body.getClauses().isEmpty())) {
      ArendExtension extension = typechecker.getExtension();
      LevelProver prover = extension == null ? null : extension.getLevelProver();
      Definition useParent = def.getUseParent() == null ? null : def.getUseParent().getTypechecked();
      if (prover != null && useParent != null && (!typedDef.getParameters().hasNext() || DependentLink.Helper.size(typedDef.getParameters()) == DependentLink.Helper.size(useParent.getParameters()))) {
        try (var ignored = new Utils.SetContextSaver<>(typechecker.getContext())) {
          boolean ok = true;
          if (typedDef.getParameters().hasNext()) {
            ExprSubstitution substitution = new ExprSubstitution();
            DependentLink useParam = useParent.getParameters();
            for (DependentLink param = typedDef.getParameters(); param.hasNext(); param = param.getNext(), useParam = useParam.getNext()) {
              if (!param.getTypeExpr().subst(substitution).isLessOrEquals(useParam.getTypeExpr(), DummyEquations.getInstance(), null)) {
                ok = false;
                break;
              }
              substitution.add(param, new ReferenceExpression(useParam));
            }
          } else {
            typedDef.setParameters(useParent.getParameters());
            for (DependentLink param = typedDef.getParameters(); param.hasNext(); param = param.getNext()) {
              typechecker.addBinding(null, param);
            }
          }
          if (ok) {
            List<Expression> args = new ArrayList<>();
            for (DependentLink param = typedDef.getParameters(); param.hasNext(); param = param.getNext()) {
              args.add(new ReferenceExpression(param));
            }
            Expression type = useParent.getDefCall(Sort.STD, args);
            Sort sort = type.getSortOfType();
            if (sort != null) {
              int level = ((UniverseExpression) typedDef.getResultType()).getSort().getHLevel().getConstant();
              CountingErrorReporter countingErrorReporter = new CountingErrorReporter(GeneralError.Level.ERROR, errorReporter);
              TypecheckingResult result = typechecker.withErrorReporter(countingErrorReporter, tc ->
                  typechecker.finalize(TypecheckingResult.fromChecked(prover.prove(body.getTerm(), type, CheckTypeVisitor.getLevelExpression(new TypeExpression(type, sort), level), level, def, tc)), def, false));
              if (result == null) {
                if (countingErrorReporter.getErrorsNumber() == 0) {
                  errorReporter.report(new TypecheckingError("Cannot prove level", def));
                }
                typedDef.setResultType(new ErrorExpression());
              } else {
                typedDef.setBody(result.expression);
                typedDef.setResultType(result.type);
              }
            }
          }
        }
      }
    } else if (body instanceof Concrete.ElimFunctionBody) {
      Concrete.ElimFunctionBody elimBody = (Concrete.ElimFunctionBody) body;
      List<DependentLink> elimParams = ElimTypechecking.getEliminatedParameters(elimBody.getEliminatedReferences(), elimBody.getClauses(), typedDef.getParameters(), typechecker);
      PatternTypechecking patternTypechecking = new PatternTypechecking(errorReporter, PatternTypechecking.Mode.FUNCTION, typechecker, true);
      clauses = elimParams == null ? null : patternTypechecking.typecheckClauses(elimBody.getClauses(), def.getParameters(), typedDef.getParameters(), elimParams, expectedType);
      Sort sort = expectedType.getSortOfType();
      Body typedBody = clauses == null ? null : new ElimTypechecking(errorReporter, typechecker.getEquations(), expectedType, PatternTypechecking.Mode.FUNCTION, typeLevel, sort != null ? sort.getHLevel() : Level.INFINITY, kind.isSFunc(), elimBody.getClauses(), def).typecheckElim(clauses, def.getParameters(), typedDef.getParameters(), elimParams);
      if (typedBody != null) {
        if (newDef) {
          typedDef.setBody(typedBody);
          typedDef.addStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        }
        boolean conditionsResult = typedDef.getKind() == CoreFunctionDefinition.Kind.LEMMA || new ConditionsChecking(DummyEquations.getInstance(), errorReporter, def).check(typedBody, clauses, elimBody.getClauses(), typedDef);
        if (newDef && !conditionsResult) {
          typedDef.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
      } else {
        clauses = null;
      }
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      if (def.getResultType() != null) {
        if (isTypeClassRef(def.getResultType(), true)) {
          Pair<ClassCallExpression, ClassCallExpression> result = typecheckCoClauses(typedDef, def, kind, body.getCoClauseElements());
          if (result != null) {
            if (newDef && !def.isRecursive()) {
              if (kind == FunctionKind.CONS) {
                typedDef.setResultType(result.proj1);
              } else {
                typedDef.setResultType(result.proj2);
                if (typedDef.isSFunc() && result.proj1.getImplementedHere().size() != result.proj2.getImplementedHere().size()) {
                  typedDef.setBody(new NewExpression(null, result.proj1));
                }
              }
            }
            consType = result.proj2;
          }
        } else {
          TypecheckingResult result = typechecker.finalCheckExpr(def.getResultType(), Type.OMEGA);
          if (newDef && result != null) {
            typedDef.setResultType(result.expression);
            typedDef.addStatus(typechecker.getStatus());
          }
        }
        bodyIsOK = true;
      }
    } else if (body instanceof Concrete.TermFunctionBody) {
      Concrete.Expression bodyTerm = ((Concrete.TermFunctionBody) body).getTerm();
      boolean useExpectedType = !expectedType.isError();
      TypecheckingResult nonFinalResult = typechecker.checkExpr(bodyTerm, useExpectedType ? expectedType : null);
      if (useExpectedType && (kind == FunctionKind.LEMMA || nonFinalResult == null || !nonFinalResult.type.isInstance(ClassCallExpression.class)) && !(expectedType instanceof Type && ((Type) expectedType).isOmega())) {
        if (nonFinalResult == null) {
          nonFinalResult = new TypecheckingResult(null, expectedType);
        } else {
          nonFinalResult.type = expectedType;
        }
      }
      TypecheckingResult termResult = typechecker.finalize(nonFinalResult, bodyTerm, kind == FunctionKind.LEMMA);

      if (termResult != null) {
        if (newDef) {
          typedDef.setResultType(termResult.type);
        }
        if (termResult.expression != null) {
          if (newDef && !def.isRecursive()) {
            typedDef.setBody(termResult.expression);
          }
        }
        if (termResult.expression instanceof FunCallExpression && ((FunCallExpression) termResult.expression).getDefinition().getActualBody() == null && ((FunCallExpression) termResult.expression).getDefinition().status() != Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
          bodyIsOK = true;
          if (newDef) {
            typedDef.hideBody();
          }
        }
        if (termResult.expression instanceof NewExpression) {
          bodyIsOK = true;
          if (newDef && (expectedType.isError() || !typedDef.isSFunc())) {
            typedDef.setBody(null);
            if (!def.isRecursive()) {
              typedDef.setResultType(((NewExpression) termResult.expression).getType());
            }
          }
        }
      }
    } else {
      throw new IllegalStateException();
    }

    if (!checkElimBody(def)) {
      typedDef.setBody(null);
    }

    if (newDef) {
      if (kind != FunctionKind.LEMMA && kind != FunctionKind.LEVEL && typedDef.getBody() instanceof DefCallExpression) {
        Integer level = ((DefCallExpression) typedDef.getBody()).getUseLevel();
        if (level != null) {
          typedDef.addParametersLevel(new ParametersLevel(null, level));
        }
      }

      if (expectedType.isError() && typedDef.getResultType() != null) {
        typedDef.getResultType().accept(goodThisParametersVisitor, null);
      }
      if (typedDef.getResultType() == null) {
        typedDef.setResultType(new ErrorExpression());
      }

      ElimBody elimBody;
      if (typedDef.getActualBody() instanceof ElimBody) {
        elimBody = (ElimBody) typedDef.getActualBody();
      } else if (typedDef.getActualBody() instanceof IntervalElim) {
        elimBody = ((IntervalElim) typedDef.getActualBody()).getOtherwise();
      } else {
        elimBody = null;
      }

      if (elimBody != null) {
        goodThisParametersVisitor = new GoodThisParametersVisitor(elimBody, DependentLink.Helper.size(typedDef.getParameters()));
      } else if (typedDef.getActualBody() instanceof Expression) {
        goodThisParametersVisitor = new GoodThisParametersVisitor((Expression) typedDef.getActualBody(), typedDef.getParameters());
      } else {
        goodThisParametersVisitor.visitBody(typedDef.getActualBody(), null);
      }
      typedDef.setGoodThisParameters(goodThisParametersVisitor.getGoodParameters());

      if (checkForUniverses(typedDef.getParameters()) || new UniverseInParametersChecker().check(typedDef.getResultType())) {
        typedDef.setUniverseKind(UniverseKind.WITH_UNIVERSES);
      } else {
        typedDef.setUniverseKind(new UniverseKindChecker().getUniverseKind(typedDef.getBody()));
      }
    }

    if (!checkLevelNow) {
      checkTypeLevel(def, typedDef, newDef);
    }

    if (kind == FunctionKind.INSTANCE) {
      ClassCallExpression typecheckedResultType = typedDef.getResultType() instanceof ClassCallExpression ? (ClassCallExpression) typedDef.getResultType() : null;
      if (typecheckedResultType != null && !typecheckedResultType.getDefinition().isRecord()) {
        ClassField classifyingField = typecheckedResultType.getDefinition().getClassifyingField();
        Expression classifyingExpr;
        if (classifyingField != null) {
          classifyingExpr = typecheckedResultType.getImplementation(classifyingField, new NewExpression(null, typecheckedResultType));
          Set<SingleDependentLink> params = new LinkedHashSet<>();
          while (classifyingExpr instanceof LamExpression) {
            for (SingleDependentLink link = ((LamExpression) classifyingExpr).getParameters(); link.hasNext(); link = link.getNext()) {
              params.add(link);
            }
            classifyingExpr = ((LamExpression) classifyingExpr).getBody();
          }
          if (classifyingExpr != null) {
            classifyingExpr = classifyingExpr.normalize(NormalizationMode.WHNF);
          }

          boolean ok = classifyingExpr == null || classifyingExpr instanceof ErrorExpression || classifyingExpr instanceof DataCallExpression || classifyingExpr instanceof ConCallExpression || classifyingExpr instanceof ClassCallExpression || classifyingExpr instanceof UniverseExpression && params.isEmpty() || classifyingExpr instanceof SigmaExpression && params.isEmpty() || classifyingExpr instanceof IntegerExpression && params.isEmpty();
          if (classifyingExpr instanceof ClassCallExpression) {
            Map<ClassField, Expression> implemented = ((ClassCallExpression) classifyingExpr).getImplementedHere();
            if (implemented.size() < params.size()) {
              ok = false;
            } else {
              int i = 0;
              ClassDefinition classDef = ((ClassCallExpression) classifyingExpr).getDefinition();
              Iterator<SingleDependentLink> it = params.iterator();
              Set<Binding> forbiddenBindings = new HashSet<>(params);
              forbiddenBindings.add(((ClassCallExpression) classifyingExpr).getThisBinding());
              for (ClassField field : classDef.getFields()) {
                Expression implementation = implemented.get(field);
                if (implementation != null) {
                  if (i < implemented.size() - params.size()) {
                    if (implementation.findBinding(forbiddenBindings) != null) {
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
          } else if (classifyingExpr instanceof DefCallExpression) {
            DefCallExpression defCall = (DefCallExpression) classifyingExpr;
            if (defCall.getDefCallArguments().size() < params.size()) {
              ok = false;
            } else {
              int i = defCall.getDefCallArguments().size() - params.size();
              for (SingleDependentLink param : params) {
                if (!(defCall.getDefCallArguments().get(i) instanceof ReferenceExpression && ((ReferenceExpression) defCall.getDefCallArguments().get(i)).getBinding() == param)) {
                  ok = false;
                  break;
                }
                i++;
              }
              if (ok && !params.isEmpty()) {
                for (i = 0; i < defCall.getDefCallArguments().size() - params.size(); i++) {
                  if (defCall.getDefCallArguments().get(i).findBinding(params) != null) {
                    ok = false;
                    break;
                  }
                }
              }
            }
          }
          if (!ok) {
            errorReporter.report(new TypecheckingError("Classifying field must be either a universe, a sigma type, a record, or a partially applied data or constructor", def.getResultType() == null ? def : def.getResultType()));
          }
        } else {
          classifyingExpr = null;
        }

        int index = 0;
        for (DependentLink link = typedDef.getParameters(); link.hasNext(); link = link.getNext()) {
          if (link instanceof TypedDependentLink && typedDef.getTypeClassParameterKind(index) == Definition.TypeClassParameterKind.YES) {
            Expression type = link.getTypeExpr();
            if (type instanceof ClassCallExpression && !((ClassCallExpression) type).getDefinition().isRecord()) {
              ClassCallExpression classCall = (ClassCallExpression) type;
              ClassField paramClassifyingField = classCall.getDefinition().getClassifyingField();
              ReferenceExpression refExpr = new ReferenceExpression(link);
              Expression classifyingImpl = paramClassifyingField == null ? null : classCall.getImplementation(paramClassifyingField, refExpr);
              Expression classifyingExprType = paramClassifyingField == null ? null : paramClassifyingField.getType(classCall.getSortArgument()).applyExpression(refExpr);
              if (classifyingImpl == null && paramClassifyingField != null) {
                classifyingImpl = FieldCallExpression.make(paramClassifyingField, classCall.getSortArgument(), refExpr);
              }
              if (classifyingImpl == null || classifyingExpr == null || compareExpressions(classifyingImpl, classifyingExpr, classifyingExprType) != -1) {
                typedDef.setTypeClassParameter(index, Definition.TypeClassParameterKind.ONLY_LOCAL);
              }
            }
          }
          index++;
        }
      }
    }

    if (typedDef instanceof DConstructor) {
      Set<DependentLink> usedVars = new HashSet<>();
      ExpressionPattern pattern = null;
      if (body instanceof Concrete.TermFunctionBody) {
        if (typedDef.getBody() instanceof Expression) {
          pattern = checkDConstructor((Expression) typedDef.getBody(), usedVars, body.getTerm());
        }
      } else if (body instanceof Concrete.CoelimFunctionBody) {
        if (consType != null && typedDef.getResultType() instanceof ClassCallExpression) {
          pattern = checkDConstructor(consType, new NewExpression(null, (ClassCallExpression) typedDef.getResultType()), usedVars, def);
        }
      } else {
        errorReporter.report(new TypecheckingError("\\cons cannot be defined by pattern matching", def));
      }

      if (pattern != null) {
        int numberOfParameters = 0;
        for (DependentLink link = typedDef.getParameters(); link.hasNext(); link = link.getNext()) {
          if (usedVars.contains(link)) {
            break;
          }
          numberOfParameters++;
        }

        for (DependentLink link = DependentLink.Helper.get(typedDef.getParameters(), numberOfParameters); link.hasNext(); link = link.getNext()) {
          if (!usedVars.contains(link)) {
            errorReporter.report(new TypecheckingError("Parameters of \\cons that do not occur in patterns must be listed before other parameters", def));
            pattern = null;
            break;
          }
        }

        if (pattern != null) {
          DependentLink link = typedDef.getParameters();
          for (int i = 0; i < numberOfParameters; i++) {
            if (link.isExplicit()) {
              errorReporter.report(new TypecheckingError("Parameters of \\cons that do not occur in patterns must be implicit", def));
              pattern = null;
              break;
            }
            if (!typedDef.getResultType().findBinding(link)) {
              if (!typedDef.getResultType().isError()) {
                errorReporter.report(new TypecheckingError("Parameters of \\cons that do not occur in patterns must occur in the result type", def));
              }
              pattern = null;
              break;
            }
            link = link.getNext();
          }
        }

        if (newDef && pattern != null) {
          ((DConstructor) typedDef).setPattern(pattern);
          ((DConstructor) typedDef).setNumberOfParameters(numberOfParameters);
        }
      }
    }

    if (newDef) {
      typechecker.setStatus(def.getStatus().getTypecheckingStatus());
      typedDef.addStatus(typechecker.getStatus().max(!bodyIsOK && typedDef.getActualBody() == null ? Definition.TypeCheckingStatus.HAS_ERRORS : Definition.TypeCheckingStatus.NO_ERRORS));
      def.setTypechecked();
    }

    return clauses;
  }

  private void typecheckDataHeader(DataDefinition dataDefinition, Concrete.DataDefinition def, LocalInstancePool localInstancePool, boolean newDef) {
    if (def.enclosingClass != null) {
      dataDefinition.setHasEnclosingClass(true);
    }
    LinkList list = new LinkList();

    Sort userSort = null;
    boolean paramsOk = typecheckParameters(def, list, localInstancePool, null, newDef || dataDefinition == null ? null : dataDefinition.getParameters(), null) != null;

    if (def.getUniverse() != null) {
      Type userTypeResult = typechecker.finalCheckType(def.getUniverse(), Type.OMEGA, false);
      if (userTypeResult != null) {
        userSort = userTypeResult.getExpr().toSort();
        if (userSort == null) {
          errorReporter.report(new TypecheckingError("Expected a universe", def.getUniverse()));
        }
      }
    }

    if (!newDef) {
      return;
    }

    dataDefinition.setParameters(list.getFirst());
    dataDefinition.setSort(userSort);
    def.getData().setTypecheckedIfAbsent(dataDefinition);
    calculateTypeClassParameters(def, dataDefinition);
    calculateParametersTypecheckingOrder(dataDefinition);

    if (!paramsOk) {
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          constructor.getData().setTypechecked(new Constructor(constructor.getData(), dataDefinition));
        }
      }
    }

    dataDefinition.setStatus(Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING);
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
      elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getConstructorClauses(), dataDefinition.getParameters(), typechecker);
      if (elimParams == null) {
        dataOk = false;
      }
    }

    ErrorReporter originalErrorReporter = errorReporter;
    ErrorReporterCounter countingErrorReporter = new ErrorReporterCounter(GeneralError.Level.ERROR, originalErrorReporter);
    errorReporter = countingErrorReporter;

    if (!def.getConstructorClauses().isEmpty()) {
      Map<Referable, Binding> context = typechecker.getContext();
      PatternTypechecking dataPatternTypechecking = new PatternTypechecking(errorReporter, PatternTypechecking.Mode.DATA, typechecker, true);

      Set<TCReferable> notAllowedConstructors = new HashSet<>();
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          notAllowedConstructors.add(constructor.getData());
        }
      }

      InstancePool instancePool = typechecker.getInstancePool().getInstancePool();
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        typechecker.copyContextFrom(context);

        // Typecheck patterns and compute free bindings
        boolean patternsOK = true;
        PatternTypechecking.Result result = null;
        if (clause.getPatterns() != null) {
          if (def.getEliminatedReferences() == null) {
            originalErrorReporter.report(new TypecheckingError("Expected a constructor without patterns", clause));
            dataOk = false;
          }
          if (elimParams != null) {
            ExprSubstitution substitution = new ExprSubstitution();
            result = dataPatternTypechecking.typecheckPatterns(clause.getPatterns(), def.getParameters(), dataDefinition.getParameters(), substitution, null, elimParams, def);
            if (instancePool != null && result != null && result.hasEmptyPattern()) {
              typechecker.getInstancePool().setInstancePool(instancePool.subst(substitution));
            }
            if (result != null && result.hasEmptyPattern()) {
              originalErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.REDUNDANT_CLAUSE, clause));
              result = null;
            }
            if (result == null) {
              typechecker.copyContextFrom(context);
              patternsOK = false;
            }
          }
        } else {
          if (def.getEliminatedReferences() != null) {
            originalErrorReporter.report(new TypecheckingError("Expected constructors with patterns", clause));
            dataOk = false;
          }
        }

        // Process constructors
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          // Check that constructors do not refer to constructors defined later
          FreeReferablesVisitor visitor = new FreeReferablesVisitor(notAllowedConstructors);
          if (constructor.getResultType() != null) {
            if (constructor.getResultType().accept(visitor, null) != null) {
              errorReporter.report(new ConstructorReferenceError(constructor.getResultType()));
              constructor.setResultType(null);
            }
          }
          Iterator<Concrete.FunctionClause> it = constructor.getClauses().iterator();
          while (it.hasNext()) {
            Concrete.FunctionClause conClause = it.next();
            if (visitor.visitClause(conClause) != null) {
              errorReporter.report(new ConstructorReferenceError(conClause));
              it.remove();
            }
          }
          boolean constructorOK = patternsOK;
          if (visitor.visitParameters(constructor.getParameters()) != null) {
            errorReporter.report(new ConstructorReferenceError(constructor));
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
          List<ExpressionPattern> patterns = result == null ? null : result.getPatterns();
          Sort conSort = typecheckConstructor(constructor, patterns, dataDefinition, dataDefinitions, def.isTruncated() ? null : userSort, newDef);
          if (conSort == null) {
            dataOk = false;
            conSort = Sort.PROP;
          }

          inferredSort = inferredSort.max(conSort);
        }
      }
      typechecker.getInstancePool().setInstancePool(instancePool);

      if (inferredSort.isProp() || inferredSort.getHLevel().isVarOnly()) {
        boolean ok = true;
        for (int i = 0; i < dataDefinition.getConstructors().size(); i++) {
          List<ExpressionPattern> patterns1 = dataDefinition.getConstructors().get(i).getPatterns();
          for (int j = i + 1; j < dataDefinition.getConstructors().size(); j++) {
            List<ExpressionPattern> patterns2 = dataDefinition.getConstructors().get(j).getPatterns();
            if (patterns1 == null || patterns2 == null || ExpressionPattern.unify(patterns1, patterns2, null, null, null, errorReporter, def)) {
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
    if (newDef && !dataOk) {
      dataDefinition.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
    }

    errorReporter = originalErrorReporter;

    // Check if constructors pattern match on the interval
    for (Constructor constructor : dataDefinition.getConstructors()) {
      if (constructor.getBody() instanceof IntervalElim && !inferredSort.getHLevel().isInfinity()) {
        inferredSort = new Sort(inferredSort.getPLevel(), Level.INFINITY);
      }
    }

    // Find covariant parameters
    if (newDef && dataDefinition.getParameters().hasNext()) {
      int index = 0;
      Set<DependentLink> parameters = new HashSet<>();
      for (DependentLink link = dataDefinition.getParameters(); link.hasNext(); link = link.getNext(), index++) {
        dataDefinition.setCovariant(index, true);
        parameters.add(link);
      }

      int size;
      do {
        size = parameters.size();
        getCovariantParameters(dataDefinition, parameters);

        index = 0;
        for (DependentLink link = dataDefinition.getParameters(); link.hasNext(); link = link.getNext(), index++) {
          dataDefinition.setCovariant(index, parameters.contains(link));
        }
      } while (!parameters.isEmpty() && parameters.size() != size);
    }

    // Check truncatedness
    if (def.isTruncated()) {
      if (userSort == null) {
        originalErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.TRUNCATED_WITHOUT_UNIVERSE, def));
      } else {
        if (inferredSort.isLessOrEquals(userSort)) {
          originalErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.DATA_WONT_BE_TRUNCATED, def.getUniverse() == null ? def : def.getUniverse()));
        } else if (newDef) {
          dataDefinition.setTruncatedLevel(userSort.getHLevel().getConstant());
          dataDefinition.setSquashed(true);
        }
      }
    } else if (countingErrorReporter.getErrorsNumber() == 0 && userSort != null && !inferredSort.isLessOrEquals(userSort)) {
      countingErrorReporter.report(new DataUniverseError(inferredSort, userSort, def.getUniverse() == null ? def : def.getUniverse()));
    }

    if (newDef) {
      Sort originalSort = dataDefinition.getSort();
      dataDefinition.setSort(countingErrorReporter.getErrorsNumber() == 0 && userSort != null ? userSort : inferredSort);
      typechecker.setStatus(def.getStatus().getTypecheckingStatus());
      dataDefinition.addStatus(typechecker.getStatus());

      if (!originalSort.equals(dataDefinition.getSort()) && (def.isRecursive() || dataDefinitions.size() > 1)) {
        for (Constructor constructor : dataDefinition.getConstructors()) {
          for (DependentLink link = constructor.getParameters(); link.hasNext(); link = link.getNext()) {
            link = link.getNextTyped(null);
            Type updated = fixTypeSorts(link.getType(), dataDefinition.getSort(), dataDefinitions);
            if (updated != null) {
              link.setType(updated);
            }
          }
        }
      }

      if (checkForUniverses(dataDefinition.getParameters())) {
        dataDefinition.setUniverseKind(UniverseKind.WITH_UNIVERSES);
      } else {
        UniverseKind kind = UniverseKind.NO_UNIVERSES;
        loop:
        for (Constructor constructor : dataDefinition.getConstructors()) {
          for (DependentLink link = constructor.getParameters(); link.hasNext(); link = link.getNext()) {
            link = link.getNextTyped(null);
            kind = kind.max(new UniverseKindChecker().getUniverseKind(link.getTypeExpr()));
            if (kind == UniverseKind.WITH_UNIVERSES) {
              break loop;
            }
          }
        }
        dataDefinition.setUniverseKind(kind);
      }
    }

    if (newDef) {
      for (Constructor constructor : dataDefinition.getConstructors()) {
        goodThisParametersVisitor.visitParameters(constructor.getParameters(), null);
        goodThisParametersVisitor.visitBody(constructor.getBody(), null);
      }
      dataDefinition.setGoodThisParameters(goodThisParametersVisitor.getGoodParameters());
      def.setTypechecked();
    }

    return countingErrorReporter.getErrorsNumber() == 0;
  }

  private Type fixTypeSorts(Type type, Sort sort, Set<DataDefinition> dataDefinitions) {
    Expression result = fixExpressionSorts(type.getExpr(), sort, dataDefinitions);
    return result == null ? null : result instanceof Type ? (Type) result : new TypeExpression(result, sort);
  }

  // fixes sorts of expressions containing recursive calls of a data type
  // See BaseDefinitionTypechecker.checkPositiveness, CheckForUniversesVisitor, and checkForContravariantUniverses
  private Expression fixExpressionSorts(Expression type, Sort sort, Set<DataDefinition> dataDefinitions) {
    if (type instanceof PiExpression) {
      PiExpression piType = (PiExpression) type;
      Expression codomain = fixExpressionSorts(piType.getCodomain(), sort, dataDefinitions);
      return codomain == null ? null : new PiExpression(sort.max(piType.getResultSort()), piType.getParameters(), codomain);
    }

    if (type instanceof SigmaExpression) {
      boolean updated = false;
      SigmaExpression sigmaExpr = (SigmaExpression) type;
      for (DependentLink link = sigmaExpr.getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        Type newType = fixTypeSorts(link.getType(), sort, dataDefinitions);
        if (newType != null) {
          link.setType(newType);
          updated = true;
        }
      }
      return updated ? new SigmaExpression(sort.max(sigmaExpr.getSort()), sigmaExpr.getParameters()) : null;
    }

    if (type instanceof FunCallExpression) {
      FunCallExpression funCall = (FunCallExpression) type;
      if (funCall.getDefinition() != Prelude.PATH_INFIX) {
        return null;
      }

      Expression newArg = fixExpressionSorts(funCall.getDefCallArguments().get(0), sort, dataDefinitions);
      if (newArg == null) {
        return null;
      }

      List<Expression> args = new ArrayList<>();
      args.add(newArg);
      args.add(funCall.getDefCallArguments().get(1));
      args.add(funCall.getDefCallArguments().get(2));
      return FunCallExpression.make(Prelude.PATH_INFIX, sort.max(funCall.getSortArgument()), args);
    }

    if (type instanceof DataCallExpression) {
      DataCallExpression dataCall = (DataCallExpression) type;
      List<Expression> args = dataCall.getDefCallArguments();
      boolean updated = false;
      for (int i = 0; i < args.size(); i++) {
        if (!dataCall.getDefinition().isCovariant(i)) {
          continue;
        }

        Expression newArg = fixExpressionSorts(args.get(i), sort, dataDefinitions);
        if (newArg != null) {
          args.set(i, newArg);
          updated = true;
        }
      }

      return updated ? new DataCallExpression(dataCall.getDefinition(), sort.max(dataCall.getSortArgument()), args) : dataDefinitions.contains(dataCall.getDefinition()) ? dataCall : null;
    }

    return null;
  }

  private Expression normalizePathExpression(Expression type, Constructor constructor, Concrete.SourceNode sourceNode) {
    type = type.normalize(NormalizationMode.WHNF);
    if (type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.PATH) {
      List<Expression> pathArgs = ((DataCallExpression) type).getDefCallArguments();
      Expression lamExpr = pathArgs.get(0).normalize(NormalizationMode.WHNF);
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
    if (type == null || !Expression.compare(type, expectedType, Type.OMEGA, CMP.EQ)) {
      errorReporter.report(new TypecheckingError("Expected an iterated path type in " + expectedType, sourceNode));
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
      expression = FunCallExpression.make(Prelude.AT, ((DataCallExpression) type).getSortArgument(), args);
      type = lamExpr.getBody();
      param = param.getNext();
    }
    return expression;
  }

  private boolean checkConstructorsOnlyOnTop(Expression expr, DataDefinition dataDefinition) {
    if (expr instanceof ConCallExpression && ((ConCallExpression) expr).getDefinition().getDataType() == dataDefinition) {
      for (Expression argument : ((ConCallExpression) expr).getDataTypeArguments()) {
        if (!checkConstructorsOnlyOnTop(argument, dataDefinition)) {
          return false;
        }
      }
      for (Expression argument : ((ConCallExpression) expr).getDefCallArguments()) {
        if (!checkConstructorsOnlyOnTop(argument, dataDefinition)) {
          return false;
        }
      }
      return true;
    }

    return !expr.accept(new ProcessDefCallsVisitor<Void>() {
      @Override
      protected boolean processDefCall(DefCallExpression expression, Void param) {
        return expression instanceof ConCallExpression && ((ConCallExpression) expression).getDefinition().getDataType() == dataDefinition;
      }
    }, null);
  }

  private void checkConstructorsOnlyOnTop(Expression expr, DataDefinition dataDefinition, Concrete.SourceNode sourceNode) {
    if (expr != null && !checkConstructorsOnlyOnTop(expr, dataDefinition)) {
      errorReporter.report(new TypecheckingError("Constructors can occur only on the top level of conditions", sourceNode));
    }
  }

  private Sort typecheckConstructor(Concrete.Constructor def, List<ExpressionPattern> patterns, DataDefinition dataDefinition, Set<DataDefinition> dataDefinitions, Sort userSort, boolean newDef) {
    Constructor constructor = newDef ? new Constructor(def.getData(), dataDefinition) : null;
    if (constructor != null) {
      constructor.setPatterns(patterns);
    }
    Constructor oldConstructor = constructor != null ? constructor : (Constructor) def.getData().getTypechecked();

    List<DependentLink> elimParams = null;
    Expression constructorType = null;
    LinkList list = new LinkList();
    Sort sort;

    try (var ignored = new Utils.SetContextSaver<>(typechecker.getContext())) {
      if (constructor != null) {
        def.getData().setTypechecked(constructor);
        dataDefinition.addConstructor(constructor);
      }

      Pair<Sort, Expression> pair = typecheckParameters(def, list, null, userSort, newDef ? null : oldConstructor.getParameters(), null);
      sort = pair == null ? null : pair.proj1;

      int i = 0;
      for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext(), i++) {
        link = link.getNextTyped(null);
        if (new RecursiveDataChecker(dataDefinitions, errorReporter, def, def.getParameters().get(i)).check(link.getTypeExpr())) {
          if (constructor != null) {
            constructor.setParameters(EmptyDependentLink.getInstance());
          }
          return null;
        }
      }

      if (def.getResultType() != null) {
        Type resultType = typechecker.finalCheckType(def.getResultType(), Type.OMEGA, false);
        if (resultType != null) {
          constructorType = normalizePathExpression(resultType.getExpr(), oldConstructor, def.getResultType());
        }
        def.setResultType(null);
      }

      if (constructor != null) {
        constructor.setParameters(list.getFirst());
      }

      if (!def.getClauses().isEmpty()) {
        elimParams = ElimTypechecking.getEliminatedParameters(def.getEliminatedReferences(), def.getClauses(), list.getFirst(), typechecker);
      }
    }

    if (elimParams != null) {
      try (var ignored = new Utils.SetContextSaver<>(typechecker.getContext())) {
        Expression expectedType = oldConstructor.getDataTypeExpression(Sort.STD);
        PatternTypechecking patternTypechecking = new PatternTypechecking(errorReporter, PatternTypechecking.Mode.CONSTRUCTOR, typechecker, true);
        List<ExtElimClause> clauses = patternTypechecking.typecheckClauses(def.getClauses(), def.getParameters(), oldConstructor.getParameters(), elimParams, expectedType);
        if (clauses != null) {
          for (int i = 0; i < clauses.size(); i++) {
            checkConstructorsOnlyOnTop(clauses.get(i).getExpression(), dataDefinition, def.getClauses().get(i).getExpression());
          }
        }
        Body body = clauses == null ? null : new ElimTypechecking(errorReporter, typechecker.getEquations(), expectedType, PatternTypechecking.Mode.CONSTRUCTOR, def.getClauses(), def).typecheckElim(clauses, def.getParameters(), oldConstructor.getParameters(), elimParams);
        if (constructor != null) {
          constructor.setBody(body);
          constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        }

        boolean dataSortIsProp = dataDefinition.getSort().isProp();
        if (dataSortIsProp) {
          dataDefinition.setSort(Sort.SET0);
        }
        if (body != null) {
          new ConditionsChecking(DummyEquations.getInstance(), errorReporter, def).check(body, clauses, def.getClauses(), oldConstructor);
        }
        if (dataSortIsProp) {
          dataDefinition.setSort(Sort.PROP);
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

        List<IntervalElim.CasePair> pairs;
        ElimBody elimBody;
        if (constructor.getBody() instanceof IntervalElim) {
          pairs = ((IntervalElim) constructor.getBody()).getCases();
          for (int i = 0; i < pairs.size(); i++) {
            pairs.set(i, new IntervalElim.CasePair(addAts(pairs.get(i).proj1, newParam, constructorType), addAts(pairs.get(i).proj2, newParam, constructorType)));
          }
          elimBody = ((IntervalElim) constructor.getBody()).getOtherwise();
        } else {
          pairs = new ArrayList<>();
          elimBody = constructor.getBody() instanceof ElimBody ? (ElimBody) constructor.getBody() : null;
        }

        while (constructorType instanceof DataCallExpression && ((DataCallExpression) constructorType).getDefinition() == Prelude.PATH) {
          List<Expression> pathArgs = ((DataCallExpression) constructorType).getDefCallArguments();
          LamExpression lamExpr = (LamExpression) pathArgs.get(0);
          constructorType = lamExpr.getBody();
          pairs.add(new IntervalElim.CasePair(addAts(pathArgs.get(1), newParam, constructorType.subst(lamExpr.getParameters(), Left())), addAts(pathArgs.get(2), newParam, constructorType.subst(lamExpr.getParameters(), Right()))));
          constructorType = constructorType.subst(lamExpr.getParameters(), new ReferenceExpression(newParam));
          newParam = newParam.getNext();
        }

        constructor.setBody(new IntervalElim(DependentLink.Helper.size(list.getFirst()), pairs, elimBody));
      }
    }

    if (constructor != null) {
      constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
      calculateTypeClassParameters(def, constructor);
      calculateGoodThisParameters(constructor);
      calculateParametersTypecheckingOrder(constructor);
    }
    return sort;
  }

  private static class LocalInstance {
    final ClassCallExpression classCall;
    final ClassField instanceField;

    LocalInstance(ClassCallExpression classCall, ClassField instanceField) {
      this.classCall = classCall;
      this.instanceField = instanceField;
    }
  }

  private ClassField findClassifyingField(ClassDefinition superClass, ClassDefinition classDef, Set<ClassDefinition> visited) {
    if (!visited.add(superClass)) {
      return null;
    }

    ClassField field = superClass.getClassifyingField();
    if (field == null) {
      return null;
    }

    if (!classDef.isImplemented(field)) {
      return field;
    }

    for (ClassDefinition superSuperClass : superClass.getSuperClasses()) {
      ClassField field1 = findClassifyingField(superSuperClass, classDef, visited);
      if (field1 != null) {
        return field1;
      }
    }

    return null;
  }

  private void typecheckClass(Concrete.ClassDefinition def, ClassDefinition typedDef, boolean newDef) {
    if (newDef) {
      typedDef.clear();
      typedDef.setUniverseKind(UniverseKind.WITH_UNIVERSES);
    }

    boolean classOk = true;

    if (newDef) {
      typedDef.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    }

    List<FieldReferable> alreadyImplementFields = new ArrayList<>();
    Concrete.SourceNode alreadyImplementedSourceNode = null;

    // Process super classes
    for (Concrete.ReferenceExpression aSuperClass : def.getSuperClasses()) {
      ClassDefinition superClass = typechecker.referableToDefinition(aSuperClass.getReferent(), ClassDefinition.class, "Expected a class", aSuperClass);
      if (superClass == null) {
        continue;
      }

      if (newDef) {
        typedDef.addFields(superClass.getFields());
        typedDef.addSuperClass(superClass);
      }

      for (Map.Entry<ClassField, AbsExpression> entry : superClass.getImplemented()) {
        if (!implementField(entry.getKey(), entry.getValue(), typedDef, alreadyImplementFields)) {
          classOk = false;
          alreadyImplementedSourceNode = aSuperClass;
        }
      }
    }

    boolean hasClassifyingField = false;
    if (!def.isRecord() && !def.withoutClassifying()) {
      if (def.getCoercingField() != null) {
        hasClassifyingField = true;
      } else {
        for (ClassDefinition superClass : typedDef.getSuperClasses()) {
          if (superClass.getClassifyingField() != null) {
            hasClassifyingField = true;
            break;
          }
        }
      }
    }

    // Check for cycles in implementations from super classes
    boolean checkImplementations = true;
    FieldDFS dfs = new FieldDFS(typedDef);
    for (ClassField field : typedDef.getFields()) {
      List<ClassField> cycle = dfs.findCycle(field);
      if (cycle != null) {
        errorReporter.report(CycleError.fromTypechecked(cycle, def));
        checkImplementations = false;
        break;
      }
    }

    // Set overridden fields from super classes
    if (!typedDef.getSuperClasses().isEmpty()) {
      // Collect overridden fields
      Set<ClassField> overriddenHere = new HashSet<>();
      for (Concrete.ClassElement element : def.getElements()) {
        if (element instanceof Concrete.OverriddenField) {
          ClassField field = typechecker.referableToClassField(((Concrete.OverriddenField) element).getOverriddenField(), null);
          if (field != null) {
            overriddenHere.add(field);
          }
        }
      }

      for (ClassField field : typedDef.getFields()) {
        if (overriddenHere.contains(field)) {
          continue;
        }

        ClassDefinition originalSuperClass = null;
        PiExpression type = null;
        for (ClassDefinition superClass : typedDef.getSuperClasses()) {
          PiExpression superType = superClass.getOverriddenType(field, Sort.STD);
          if (superType != null) {
            if (type == null) {
              originalSuperClass = superClass;
              TypedSingleDependentLink thisParam = new TypedSingleDependentLink(false, "this", new ClassCallExpression(typedDef, Sort.STD), true);
              type = new PiExpression(superType.getResultSort(), thisParam, superType.applyExpression(new ReferenceExpression(thisParam)));
            } else {
              if (!CompareVisitor.compare(DummyEquations.getInstance(), CMP.EQ, type.getCodomain(), superType.applyExpression(new ReferenceExpression(type.getParameters())), Type.OMEGA, def)) {
                if (!type.getCodomain().isError() && !superType.getCodomain().isError()) {
                  errorReporter.report(new TypecheckingError("The types of the field '" + field.getName() + "' differ in super classes '" + originalSuperClass.getName() + "' and '" + superClass.getName() + "'", def));
                }
                type = new PiExpression(type.getResultSort(), type.getParameters(), new ErrorExpression());
                break;
              }
            }
          }
        }
        if (newDef && type != null) {
          typedDef.overrideField(field, type);
        }
      }
    }

    // Process fields and implementations
    Concrete.Expression previousType = null;
    ClassField previousField = null;
    List<LocalInstance> localInstances = new ArrayList<>();
    Set<ClassField> implementedHere = new HashSet<>();
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassField) {
        Concrete.ClassField field = (Concrete.ClassField) element;
        if (previousType == field.getResultType()) {
          if (newDef && previousField != null) {
            ClassField newField = addField(field.getData(), typedDef, previousField.getType(Sort.STD), previousField.getTypeLevel());
            newField.setStatus(previousField.status());
            newField.setUniverseKind(previousField.getUniverseKind());
            newField.setNumberOfParameters(previousField.getNumberOfParameters());
          }
        } else {
          previousType = field.getResultType();
          previousField = typecheckClassField(field, typedDef, localInstances, newDef, hasClassifyingField);
          if (previousField != null) {
            UniverseKind universeKind = new UniverseKindChecker().getUniverseKind(previousField.getType(Sort.STD).getCodomain());
            previousField.setUniverseKind(universeKind);
            previousField.setNumberOfParameters(Concrete.getNumberOfParameters(field.getParameters()));
          }

          if (field.getData().isParameterField() && !field.getData().isExplicitField()) {
            ClassField typecheckedField = previousField != null ? previousField : (ClassField) field.getData().getTypechecked();
            if (typecheckedField.getResultType() instanceof ClassCallExpression) {
              ClassCallExpression classCall = (ClassCallExpression) typecheckedField.getResultType();
              if (!classCall.getDefinition().isRecord()) {
                localInstances.add(new LocalInstance(classCall, typecheckedField));
              }
            }
          }
        }
      } else if (element instanceof Concrete.ClassFieldImpl) {
        Concrete.ClassFieldImpl classFieldImpl = (Concrete.ClassFieldImpl) element;
        ClassField field = typechecker.referableToClassField(classFieldImpl.getImplementedField(), classFieldImpl);
        if (field == null) {
          classOk = false;
          continue;
        }
        boolean isFieldAlreadyImplemented;
        if (newDef) {
          isFieldAlreadyImplemented = typedDef.isImplemented(field);
        } else if (implementedHere.contains(field)) {
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
          implementedHere.add(field);
        }

        if (isFieldAlreadyImplemented || !checkImplementations) {
          continue;
        }

        typedDef.updateSort();

        TypedSingleDependentLink thisBinding = new TypedSingleDependentLink(false, "this", new ClassCallExpression(typedDef, Sort.STD), true);
        Concrete.LamExpression lamImpl = (Concrete.LamExpression) classFieldImpl.implementation;
        TypecheckingResult result;
        if (lamImpl != null) {
          typechecker.addBinding(lamImpl.getParameters().get(0).getReferableList().get(0), thisBinding);
          PiExpression fieldType = typedDef.getOverriddenType(field, Sort.STD);
          if (fieldType == null) {
            fieldType = field.getType(Sort.STD);
          }
          LocalInstancePool localInstancePool = new LocalInstancePool(typechecker);
          addLocalInstances(localInstances, thisBinding, !typedDef.isRecord() && typedDef.getClassifyingField() == null ? typedDef : null, localInstancePool);
          myInstancePool.setInstancePool(localInstancePool);
          CheckTypeVisitor.setCaseLevel(lamImpl.body);
          result = typechecker.finalCheckExpr(lamImpl.body, fieldType.getCodomain().subst(fieldType.getParameters(), new ReferenceExpression(thisBinding)));
          myInstancePool.setInstancePool(null);
        } else {
          result = null;
        }
        if (result == null) {
          classOk = false;
        }

        typechecker.getContext().clear();

        if (result != null) {
          List<ClassField> cycle = dfs.checkDependencies(field, FieldsCollector.getFields(result.expression, thisBinding, typedDef.getFields()));
          if (cycle != null) {
            errorReporter.report(CycleError.fromTypechecked(cycle, def));
            checkImplementations = false;
          }
        }

        if (newDef) {
          typedDef.implementField(field, new AbsExpression(thisBinding, checkImplementations && result != null ? result.expression : new ErrorExpression()));
        }
      } else if (element instanceof Concrete.OverriddenField) {
        ClassField field = typecheckClassField((Concrete.OverriddenField) element, typedDef, localInstances, newDef, hasClassifyingField);
        if (field == null) {
          classOk = false;
        }
      } else {
        throw new IllegalStateException();
      }
    }

    // Set fields covariance
    Set<ClassField> covariantFields = new HashSet<>(typedDef.getPersonalFields());
    ParametersCovarianceChecker checker = new ParametersCovarianceChecker(covariantFields);
    for (ClassField field : typedDef.getPersonalFields()) {
      checker.check(field.getType(Sort.STD).getCodomain());
      if (covariantFields.isEmpty()) {
        break;
      }
    }
    for (ClassField field : covariantFields) {
      field.setCovariant(true);
    }

    // Process classifying field
    if (!def.isRecord()) {
      ClassField classifyingField = null;
      if (!def.isForcedCoercingField() && !typedDef.getSuperClasses().isEmpty()) {
        Set<ClassDefinition> visited = new HashSet<>();
        for (ClassDefinition superClass : typedDef.getSuperClasses()) {
          classifyingField = findClassifyingField(superClass, typedDef, visited);
          if (classifyingField != null) {
            break;
          }
        }
      }
      if (classifyingField == null && def.getCoercingField() != null) {
        Definition definition = def.getCoercingField().getTypechecked();
        if (definition instanceof ClassField && ((ClassField) definition).getParentClass().equals(typedDef)) {
          classifyingField = (ClassField) definition;
        } else {
          errorReporter.report(new TypecheckingError("Internal error: coercing field must be a field belonging to the class", def));
        }
      }
      if (def.withoutClassifying()) {
        if (classifyingField == null) {
          errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.NO_CLASSIFYING_IGNORED, def));
        } else {
          classifyingField = null;
        }
      }
      if (newDef) {
        typedDef.setClassifyingField(classifyingField);
        if (classifyingField != null) {
          if (classifyingField.getParentClass() == typedDef) {
            classifyingField.setHideable(true);
            classifyingField.setType(classifyingField.getType(Sort.STD).normalize(NormalizationMode.WHNF));
          }
          typedDef.getCoerceData().addCoercingField(classifyingField);
        }
      }
    } else {
      if (newDef) {
        typedDef.setRecord();
      }
    }

    if (!alreadyImplementFields.isEmpty()) {
      errorReporter.report(new FieldsImplementationError(true, def.getData(), alreadyImplementFields, alreadyImplementFields.size() > 1 ? def : alreadyImplementedSourceNode));
    }

    if (newDef) {
      typedDef.setStatus(!classOk ? Definition.TypeCheckingStatus.HAS_ERRORS : typechecker.getStatus());
      typedDef.updateSort();

      typedDef.setUniverseKind(UniverseKind.NO_UNIVERSES);
      for (ClassField field : typedDef.getFields()) {
        if (field.getUniverseKind().ordinal() > typedDef.getUniverseKind().ordinal() && !typedDef.isImplemented(field)) {
          typedDef.setUniverseKind(field.getUniverseKind());
          if (typedDef.getUniverseKind() == UniverseKind.WITH_UNIVERSES) {
            break;
          }
        }
      }

      for (ClassField field : typedDef.getPersonalFields()) {
        field.getType(Sort.STD).getParameters().setType(new ClassCallExpression(typedDef, Sort.STD));
      }

      Set<ClassField> goodFields = new HashSet<>(typedDef.getPersonalFields());
      GoodThisParametersVisitor visitor = new GoodThisParametersVisitor(goodFields);
      for (ClassDefinition superClass : typedDef.getSuperClasses()) {
        goodFields.addAll(superClass.getGoodThisFields());
      }
      for (ClassField field : typedDef.getPersonalFields()) {
        field.getType(Sort.STD).getCodomain().accept(visitor, null);
      }
      for (Concrete.ClassElement element : def.getElements()) {
        if (element instanceof Concrete.ClassFieldImpl) {
          ClassField field = typechecker.referableToClassField(((Concrete.ClassFieldImpl) element).getImplementedField(), null);
          if (field != null) {
            AbsExpression impl = typedDef.getImplementation(field);
            if (impl != null) {
              impl.getExpression().accept(visitor, null);
            }
          }
        } else if (element instanceof Concrete.OverriddenField) {
          ClassField field = typechecker.referableToClassField(((Concrete.OverriddenField) element).getOverriddenField(), null);
          if (field != null) {
            field.getType(Sort.STD).getCodomain().accept(visitor, null);
          }
        }
      }
      typedDef.setGoodThisFields(visitor.getGoodFields());

      Set<ClassField> typeClassFields = new HashSet<>();
      for (Concrete.ClassElement element : def.getElements()) {
        if (element instanceof Concrete.ClassField && ((Concrete.ClassField) element).getData().isParameterField()) {
          Concrete.Expression resultType = ((Concrete.ClassField) element).getResultType();
          if (resultType instanceof Concrete.PiExpression) {
            resultType = ((Concrete.PiExpression) resultType).getCodomain();
          }
          if (isTypeClassRef(resultType, false)) {
            ClassField field = typechecker.referableToClassField(((Concrete.ClassField) element).getData(), null);
            if (field != null) {
              typeClassFields.add(field);
            }
          }
        }
      }
      if (!typeClassFields.isEmpty()) {
        typedDef.setTypeClassFields(typeClassFields);
      }

      def.setTypechecked();
    }
  }

  private Pair<DependentLink, Expression> addPiParametersToContext(List<? extends Concrete.Parameter> parameters, Expression piType) {
    Expression resultType = piType;
    SingleDependentLink link = EmptyDependentLink.getInstance();
    for (Concrete.Parameter parameter : parameters) {
      for (Referable referable : parameter.getReferableList()) {
        if (!link.hasNext()) {
          if (!(resultType instanceof PiExpression)) {
            return new Pair<>(link, null);
          }
          link = ((PiExpression) resultType).getParameters();
          resultType = ((PiExpression) resultType).getCodomain();
        }
        typechecker.addBinding(referable, link);
        link = link.getNext();
      }
    }
    return new Pair<>(link, resultType);
  }

  private ClassField typecheckClassField(Concrete.BaseClassField def, ClassDefinition parentClass, List<LocalInstance> localInstances, boolean newDef, boolean hasClassifyingField) {
    ClassField typedDef = null;
    if (def instanceof Concrete.OverriddenField) {
      typedDef = typechecker.referableToClassField(((Concrete.OverriddenField) def).getOverriddenField(), def);
      if (typedDef == null) {
        return null;
      }

      if (typedDef.getParentClass() == parentClass || !parentClass.getFields().contains(typedDef)) {
        errorReporter.report(new TypecheckingError("Overridden field must belong to a super class", def));
        return null;
      }
    }

    boolean isProperty = false;
    boolean ok;
    PiExpression piType;
    try (var ignored = new Utils.SetContextSaver<>(typechecker.getContext())) {
      Concrete.Expression codomain;
      TypedSingleDependentLink thisParam = new TypedSingleDependentLink(false, "this", new ClassCallExpression(parentClass, Sort.STD), true);
      if (!def.getParameters().isEmpty()) {
        if (def.getParameters().size() == 1) {
          codomain = def.getResultType();
        } else {
          codomain = new Concrete.PiExpression(def.getParameters().get(1).getData(), def.getParameters().subList(1, def.getParameters().size()), def.getResultType());
        }
        typechecker.addBinding(def.getParameters().get(0).getReferableList().get(0), thisParam);
      } else {
        typechecker.addBinding(null, thisParam);
        errorReporter.report(new TypecheckingError("Internal error: class field must have a function type", def));
        codomain = def.getResultType();
      }

      LocalInstancePool localInstancePool = new LocalInstancePool(typechecker);
      addLocalInstances(localInstances, thisParam, !parentClass.isRecord() && !hasClassifyingField ? parentClass : null, localInstancePool);
      myInstancePool.setInstancePool(localInstancePool);
      ClassFieldKind kind = def instanceof Concrete.ClassField ? ((Concrete.ClassField) def).getKind() : typedDef == null ? ClassFieldKind.ANY : typedDef.isProperty() ? ClassFieldKind.PROPERTY : ClassFieldKind.FIELD;
      Type typeResult = typechecker.finalCheckType(codomain, Type.OMEGA, kind == ClassFieldKind.PROPERTY && def.getResultTypeLevel() == null);
      myInstancePool.setInstancePool(null);
      ok = typeResult != null;
      Expression typeExpr = ok ? typeResult.getExpr() : new ErrorExpression();
      piType = new PiExpression(ok ? Sort.STD.max(typeResult.getSortOfType()) : Sort.STD, thisParam, typeExpr);

      if (newDef && def instanceof Concrete.ClassField) {
        typedDef = addField(((Concrete.ClassField) def).getData(), parentClass, piType, null);
      }

      if (ok && def.getResultTypeLevel() != null) {
        if (kind == ClassFieldKind.FIELD) {
          errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.LEVEL_IGNORED, def.getResultTypeLevel()));
        } else {
          var pair = addPiParametersToContext(def.getParameters(), piType);
          if (!pair.proj1.hasNext() && pair.proj2 != null) {
            Integer level = typecheckResultTypeLevel(def.getResultTypeLevel(), false, true, pair.proj2, null, typedDef, newDef && def instanceof Concrete.ClassField);
            isProperty = level != null && level == -1;
          } else {
            // Just reports an error
            typechecker.getExpressionLevel(pair.proj1, null, null, DummyEquations.getInstance(), def.getResultTypeLevel());
          }
        }
      } else if (ok && kind != ClassFieldKind.FIELD) {
        Sort sort = typeResult.getSortOfType();
        if (sort.isProp()) {
          isProperty = true;
        } else {
          DefCallExpression defCall = typeExpr.cast(DefCallExpression.class);
          Integer level = defCall == null ? null : defCall.getUseLevel();
          if (kind == ClassFieldKind.PROPERTY) {
            boolean check = true;
            if ((level == null || level != -1) && typechecker.getExtension() != null) {
              LevelProver prover = typechecker.getExtension().getLevelProver();
              if (prover != null) {
                var pair = addPiParametersToContext(def.getParameters(), piType);
                Type typeType;
                Expression type = pair.proj2;
                if (type == null) {
                  type = typeExpr;
                  typeType = typeResult;
                } else if (type instanceof Type) {
                  typeType = (Type) type;
                } else {
                  Sort typeSort = type.getSortOfType();
                  typeType = new TypeExpression(type, typeSort != null ? typeSort : typeResult.getSortOfType());
                }
                TypecheckingResult result = pair.proj2 == null ? null : typechecker.finalize(TypecheckingResult.fromChecked(prover.prove(null, type, CheckTypeVisitor.getLevelExpression(typeType, -1), -1, def.getResultType(), typechecker)), def.getResultType(), false);
                if (result != null) {
                  Integer level2 = checkResultTypeLevel(result, false, true, type, null, typedDef, newDef, def);
                  if (level2 != null && level2 == -1) {
                    isProperty = true;
                  }
                  check = false;
                }
              }
            }
            if (check && checkLevel(false, true, level, sort, def)) {
              isProperty = true;
            }
          } else {
            if (level != null && level == -1) {
              isProperty = true;
            }
          }
        }
      }
    }

    if (newDef && typedDef == null) {
      throw new IllegalStateException();
    }

    GoodThisParametersVisitor goodThisParametersVisitor = new GoodThisParametersVisitor(piType.getParameters());
    piType.getCodomain().accept(goodThisParametersVisitor, null);
    List<Boolean> goodThisParams = goodThisParametersVisitor.getGoodParameters();
    if (goodThisParams.isEmpty() || !goodThisParams.get(0)) {
      errorReporter.report(new TypecheckingError("The type of the field contains illegal \\this occurrence", def.getParameters().isEmpty() ? def.getResultType() : def.getParameters().get(0)));
      ok = false;
      if (newDef && def instanceof Concrete.ClassField) {
        typedDef.setType(new PiExpression(piType.getResultSort(), piType.getParameters(), new ErrorExpression()));
      }
    }

    if (def instanceof Concrete.OverriddenField) {
      if (!CompareVisitor.compare(DummyEquations.getInstance(), CMP.LE, piType.getCodomain(), typedDef.getType(Sort.STD).applyExpression(new ReferenceExpression(piType.getParameters())), Type.OMEGA, def)) {
        if (!piType.getCodomain().isError() && !typedDef.getType(Sort.STD).getCodomain().isError()) {
          errorReporter.report(new TypecheckingError("The type of the overridden field is not compatible with the specified type", def));
        }
        ok = false;
      }
      for (ClassDefinition superClass : parentClass.getSuperClasses()) {
        if (!ok) {
          break;
        }
        PiExpression superType = superClass.getOverriddenType(typedDef, Sort.STD);
        if (superType != null && !CompareVisitor.compare(DummyEquations.getInstance(), CMP.LE, piType.getCodomain(), superType.applyExpression(new ReferenceExpression(piType.getParameters())), Type.OMEGA, def)) {
          if (!piType.getCodomain().isError() && !superType.getCodomain().isError()) {
            errorReporter.report(new TypecheckingError("The type of the field in super class '" + superClass.getName() + "' is not compatible with the specified type", def));
          }
          ok = false;
        }
      }
      if (newDef) {
        parentClass.overrideField(typedDef, ok ? piType : new PiExpression(piType.getResultSort(), piType.getParameters(), new ErrorExpression()));
      }
      if (!ok) {
        return null;
      }
    }

    if (!newDef) {
      return null;
    }

    if (isProperty && def instanceof Concrete.ClassField) {
      typedDef.setIsProperty();
    }
    if (!ok) {
      typedDef.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
    }
    return typedDef;
  }

  private void addLocalInstances(List<LocalInstance> localInstances, Binding thisParam, ClassDefinition classDef, LocalInstancePool localInstancePool) {
    if (localInstances.isEmpty() && classDef == null) {
      return;
    }

    if (classDef != null) {
      localInstancePool.addLocalInstance(null, classDef, new ReferenceExpression(thisParam));
    }
    for (LocalInstance localInstance : localInstances) {
      ClassField classifyingField = localInstance.classCall.getDefinition().getClassifyingField();
      Expression instance = FieldCallExpression.make(localInstance.instanceField, Sort.STD, new ReferenceExpression(thisParam));
      if (classifyingField == null) {
        localInstancePool.addLocalInstance(null, localInstance.classCall.getDefinition(), instance);
      } else {
        localInstancePool.addLocalInstance(FieldCallExpression.make(classifyingField, localInstance.classCall.getSortArgument(), instance), localInstance.classCall.getDefinition(), instance);
      }
    }
  }

  private ClassField addField(TCFieldReferable fieldRef, ClassDefinition parentClass, PiExpression piType, Expression typeLevel) {
    ClassField typedDef = new ClassField(fieldRef, parentClass, piType, typeLevel);
    fieldRef.setTypechecked(typedDef);
    parentClass.addField(typedDef);
    parentClass.addPersonalField(typedDef);
    return typedDef;
  }

  private static boolean implementField(ClassField classField, AbsExpression implementation, ClassDefinition classDef, List<FieldReferable> alreadyImplemented) {
    AbsExpression oldImpl = classDef.implementField(classField, implementation);
    ReferenceExpression thisRef = new ReferenceExpression(classField.getType(Sort.STD).getParameters());
    if (oldImpl != null && !classField.isProperty() && !Expression.compare(oldImpl.apply(thisRef), implementation.apply(thisRef), classField.getType(Sort.STD).getCodomain(), CMP.EQ)) {
      alreadyImplemented.add(classField.getReferable());
      return false;
    } else {
      return true;
    }
  }

  private int compareExpressions(Expression expr1, Expression expr2, Expression type) {
    if (expr2 instanceof ErrorExpression) {
      return 1;
    }
    expr1 = expr1.normalize(NormalizationMode.WHNF);

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
        ExprSubstitution substitution = new ExprSubstitution();
        DependentLink link = ((DataCallExpression) expr1).getDefinition().getParameters();
        List<Expression> args1 = ((DataCallExpression) expr1).getDefCallArguments();
        List<Expression> args2 = ((DataCallExpression) expr2).getDefCallArguments();
        for (int i = 0; i < args1.size(); i++) {
          int argCmp = compareExpressions(args1.get(i), args2.get(i), link.getTypeExpr().subst(substitution));
          if (argCmp == 1) {
            cmp = 1;
            break;
          }
          if (argCmp == -1) {
            cmp = -1;
          }

          substitution.add(link, args1.get(i));
          link = link.getNext();
        }
        if (cmp == -1) {
          return -1;
        }
      }

      for (Expression arg : ((DataCallExpression) expr2).getDefCallArguments()) {
        if (compareExpressions(expr1, arg, null) != 1) {
          return -1;
        }
      }

      return cmp;
    }

    if (expr2 instanceof ClassCallExpression) {
      int cmp = 0;
      if (expr1 instanceof ClassCallExpression && ((ClassCallExpression) expr1).getDefinition() == ((ClassCallExpression) expr2).getDefinition() && ((ClassCallExpression) expr1).getImplementedHere().size() == ((ClassCallExpression) expr2).getImplementedHere().size()) {
        for (Map.Entry<ClassField, Expression> entry : ((ClassCallExpression) expr1).getImplementedHere().entrySet()) {
          Expression impl2 = ((ClassCallExpression) expr2).getAbsImplementationHere(entry.getKey());
          if (impl2 == null) {
            cmp = 1;
            break;
          }

          int argCmp = compareExpressions(entry.getValue(), impl2, null);
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
        if (compareExpressions(expr1, arg, null) != 1) {
          return -1;
        }
      }

      return cmp;
    }

    return Expression.compare(expr1, expr2, type, CMP.EQ) ? 0 : 1;
  }
}
