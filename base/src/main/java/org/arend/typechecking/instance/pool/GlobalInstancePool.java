package org.arend.typechecking.instance.pool;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.instance.InstanceSearchParameters;
import org.arend.naming.reference.CoreReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.ext.util.Pair;

import java.util.*;
import java.util.function.Predicate;

public class GlobalInstancePool implements InstancePool {
  private final InstanceProvider myInstanceProvider;
  private final CheckTypeVisitor myCheckTypeVisitor;
  private LocalInstancePool myInstancePool;

  public GlobalInstancePool(InstanceProvider instanceProvider, CheckTypeVisitor checkTypeVisitor) {
    myInstanceProvider = instanceProvider;
    myCheckTypeVisitor = checkTypeVisitor;
  }

  public GlobalInstancePool(InstanceProvider instanceProvider, CheckTypeVisitor checkTypeVisitor, LocalInstancePool instancePool) {
    myInstanceProvider = instanceProvider;
    myCheckTypeVisitor = checkTypeVisitor;
    myInstancePool = instancePool;
  }

  public void setInstancePool(LocalInstancePool instancePool) {
    myInstancePool = instancePool;
  }

  public InstanceProvider getInstanceProvider() {
    return myInstanceProvider;
  }

  @Override
  public LocalInstancePool getLocalInstancePool() {
    return myInstancePool;
  }

  @Override
  public Expression addLocalInstance(Expression classifyingExpression, ClassDefinition classDef, Expression instance) {
    return myInstancePool.addLocalInstance(classifyingExpression, classDef, instance);
  }

  @Override
  public List<?> getLocalInstances() {
    return myInstancePool == null ? Collections.emptyList() : myInstancePool.getLocalInstances();
  }

  @Override
  public GlobalInstancePool copy(CheckTypeVisitor typechecker) {
    return new GlobalInstancePool(myInstanceProvider, typechecker, myInstancePool == null ? null : myInstancePool.copy(typechecker));
  }

  @Override
  public TypecheckingResult findInstance(Expression classifyingExpression, Expression expectedType, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression, Definition currentDef) {
    if (myInstancePool != null) {
      TypecheckingResult result = myInstancePool.findInstance(classifyingExpression, expectedType, parameters, sourceNode, currentDef, currentDef instanceof ClassDefinition ? LocalInstancePool.FieldSearchParameters.ALL : LocalInstancePool.FieldSearchParameters.NOT_FIELDS);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider != null) {
      Pair<Concrete.Expression, ClassDefinition> pair = getInstancePair(classifyingExpression, parameters, sourceNode, recursiveHoleExpression, currentDef);
      if (pair != null) {
        if (expectedType == null) {
          ClassCallExpression classCall = classifyingExpression == null ? null : new ClassCallExpression(pair.proj2, pair.proj2.generateInferVars(myCheckTypeVisitor.getEquations(), sourceNode));
          if (classCall != null) {
            myCheckTypeVisitor.fixClassExtSort(classCall, sourceNode);
            expectedType = classCall;
          }
        }
        TypecheckingResult result = myCheckTypeVisitor.checkExpr(pair.proj1, expectedType);
        if (result == null) {
          ErrorExpression errorExpr = new ErrorExpression();
          return new TypecheckingResult(errorExpr, errorExpr);
        }
        return result;
      }
    }

    return myInstancePool != null && !(currentDef instanceof ClassDefinition) ? myInstancePool.findInstance(classifyingExpression, expectedType, parameters, sourceNode, currentDef, LocalInstancePool.FieldSearchParameters.FIELDS_ONLY) : null;
  }

  @Override
  public Concrete.Expression findInstance(Expression classifyingExpression, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression, Definition currentDef) {
    if (myInstancePool != null) {
      Concrete.Expression result = myInstancePool.findInstance(classifyingExpression, parameters, sourceNode, currentDef, currentDef instanceof ClassDefinition ? LocalInstancePool.FieldSearchParameters.ALL : LocalInstancePool.FieldSearchParameters.NOT_FIELDS);
      if (result != null) {
        return result;
      }
    }

    Pair<Concrete.Expression, ClassDefinition> pair = myInstanceProvider == null ? null : getInstancePair(classifyingExpression, parameters, sourceNode, recursiveHoleExpression, currentDef);
    return pair != null ? pair.proj1 : myInstancePool != null && !(currentDef instanceof ClassDefinition) ? myInstancePool.findInstance(classifyingExpression, parameters, sourceNode, currentDef, LocalInstancePool.FieldSearchParameters.FIELDS_ONLY) : null;
  }

  private boolean compareLevel(Level instanceLevel, Level inferredLevel) {
    return !(instanceLevel.isClosed() && (inferredLevel.isClosed() && instanceLevel.getConstant() != inferredLevel.getConstant() || !inferredLevel.isClosed() && !(inferredLevel.getVar() instanceof InferenceLevelVariable) || inferredLevel.getConstant() > instanceLevel.getConstant() || inferredLevel.getMaxConstant() > instanceLevel.getConstant()));
  }

  private boolean compareClassifying(Expression instanceExpr, Expression inferredExpr, boolean topLevel) {
    if (instanceExpr instanceof UniverseExpression) {
      if (!(inferredExpr instanceof UniverseExpression)) return false;
      Sort instanceSort = ((UniverseExpression) instanceExpr).getSort();
      Sort inferredSort = ((UniverseExpression) inferredExpr).getSort();
      return compareLevel(instanceSort.getPLevel(), inferredSort.getPLevel()) && compareLevel(instanceSort.getHLevel(), inferredSort.getHLevel());
    } else if (instanceExpr instanceof SigmaExpression) {
      if (!(inferredExpr instanceof SigmaExpression)) return false;
      DependentLink instanceParams = ((SigmaExpression) instanceExpr).getParameters();
      DependentLink inferredParams = ((SigmaExpression) inferredExpr).getParameters();
      if (DependentLink.Helper.size(instanceParams) != DependentLink.Helper.size(inferredParams)) return false;
      for (; instanceParams.hasNext(); instanceParams = instanceParams.getNext(), inferredParams = inferredParams.getNext()) {
        if (!compareClassifying(instanceParams.getTypeExpr(), inferredParams.getTypeExpr(), false)) return false;
      }
      return true;
    } else if (instanceExpr instanceof PiExpression instancePi) {
      if (!(inferredExpr instanceof PiExpression inferredPi)) return false;
        if (DependentLink.Helper.size(instancePi.getParameters()) != DependentLink.Helper.size(inferredPi.getParameters())) return false;
      for (DependentLink instanceParams = instancePi.getParameters(), inferredParams = inferredPi.getParameters(); instanceParams.hasNext(); instanceParams = instanceParams.getNext(), inferredParams = inferredParams.getNext()) {
        if (!compareClassifying(instanceParams.getTypeExpr(), inferredParams.getTypeExpr(), false)) return false;
      }
      return compareClassifying(instancePi.getCodomain(), inferredPi.getCodomain(), false);
    } else if (instanceExpr instanceof IntegerExpression instanceIntExpr) {
        return inferredExpr instanceof IntegerExpression && instanceIntExpr.isEqual((IntegerExpression) inferredExpr) || inferredExpr instanceof ConCallExpression && instanceIntExpr.match(((ConCallExpression) inferredExpr).getDefinition());
    } else if (instanceExpr instanceof DefCallExpression instanceDefCall && !(instanceExpr instanceof FieldCallExpression)) {
      if (!(inferredExpr instanceof DefCallExpression inferredDefCall)) return false;
        if (instanceDefCall.getDefinition() != inferredDefCall.getDefinition()) return false;
      for (int i = 0; i < instanceDefCall.getDefCallArguments().size(); i++) {
        if (!compareClassifying(instanceDefCall.getDefCallArguments().get(i), inferredDefCall.getDefCallArguments().get(i), false)) return false;
      }
      if (instanceDefCall instanceof ConCallExpression instanceConCall) {
          for (int i = 0; i < instanceConCall.getDataTypeArguments().size(); i++) {
          if (!compareClassifying(instanceConCall.getDataTypeArguments().get(i), ((ConCallExpression) inferredDefCall).getDataTypeArguments().get(i), false)) return false;
        }
      } else if (instanceDefCall instanceof ClassCallExpression) {
        for (Map.Entry<ClassField, Expression> entry : ((ClassCallExpression) instanceDefCall).getImplementedHere().entrySet()) {
          Expression impl = ((ClassCallExpression) instanceDefCall).getAbsImplementationHere(entry.getKey());
          if (impl == null || !compareClassifying(entry.getValue(), impl, false)) return false;
        }
      }
      return true;
    } else {
      return !topLevel;
    }
  }

  private Pair<Concrete.Expression, ClassDefinition> getInstancePair(Expression classifyingExpression, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression, Definition currentDef) {
    if (!parameters.searchGlobal()) {
      return null;
    }

    Expression normClassifyingExpression = classifyingExpression;
    if (classifyingExpression != null) {
      normClassifyingExpression = normClassifyingExpression.normalize(NormalizationMode.WHNF);
      while (normClassifyingExpression instanceof LamExpression) {
        normClassifyingExpression = ((LamExpression) normClassifyingExpression).getBody().normalize(NormalizationMode.WHNF);
      }
      if (!(normClassifyingExpression instanceof DefCallExpression || normClassifyingExpression instanceof SigmaExpression || normClassifyingExpression instanceof PiExpression || normClassifyingExpression instanceof UniverseExpression || normClassifyingExpression instanceof IntegerExpression)) {
        return null;
      }
    }

    Expression finalClassifyingExpression = normClassifyingExpression;
    class MyPredicate implements Predicate<TCDefReferable> {
      private FunctionDefinition instanceDef = null;

      @Override
      public boolean test(TCDefReferable instance) {
        instanceDef = (FunctionDefinition) instance.getTypechecked();
        if (!(instanceDef != null && instanceDef.status().headerIsOK() && instanceDef.getResultType() instanceof ClassCallExpression classCall && parameters.testClass(((ClassCallExpression) instanceDef.getResultType()).getDefinition()) && parameters.testGlobalInstance(instanceDef))) {
          return false;
        }

          if (finalClassifyingExpression == null || classCall.getDefinition().getClassifyingField() == null) {
          return true;
        }

        Expression instanceClassifyingExpr = classCall.getAbsImplementationHere(classCall.getDefinition().getClassifyingField());
        if (instanceClassifyingExpr != null) {
          instanceClassifyingExpr = instanceClassifyingExpr.normalize(NormalizationMode.WHNF);
        }
        while (instanceClassifyingExpr instanceof LamExpression) {
          instanceClassifyingExpr = ((LamExpression) instanceClassifyingExpr).getBody();
        }
        return compareClassifying(instanceClassifyingExpr, finalClassifyingExpression, true);
      }
    }

    MyPredicate predicate = new MyPredicate();
    TCDefReferable instance = myInstanceProvider.findInstance(predicate);
    if (instance == null || predicate.instanceDef == null) {
      return null;
    }

    ClassDefinition actualClass = ((ClassCallExpression) predicate.instanceDef.getResultType()).getDefinition();
    Object data = sourceNode == null ? null : sourceNode.getData();
    Concrete.Expression instanceExpr = new Concrete.ReferenceExpression(data, instance);
    DependentLink link = predicate.instanceDef.getParameters();
    ClassDefinition enclosingClass = currentDef != null ? currentDef.getEnclosingClass() : null;
    if (myInstancePool != null && !myInstancePool.getLocalInstances().isEmpty() && myInstancePool.getLocalInstances().get(0).classDef == enclosingClass && predicate.instanceDef.getEnclosingClass() == enclosingClass) {
      instanceExpr = Concrete.AppExpression.make(data, instanceExpr, new Concrete.ReferenceExpression(data, new CoreReferable(null, myInstancePool.getLocalInstances().get(0).value.computeTyped())), link.isExplicit());
      link = link.getNext();
    }
    for (; link.hasNext(); link = link.getNext()) {
      List<RecursiveInstanceData> newRecursiveData = new ArrayList<>((recursiveHoleExpression == null ? 0 : recursiveHoleExpression.recursiveData.size()) + 1);
      if (recursiveHoleExpression != null) {
        newRecursiveData.addAll(recursiveHoleExpression.recursiveData);
      }
      newRecursiveData.add(new RecursiveInstanceData(instance, actualClass.getReferable(), classifyingExpression));
      instanceExpr = Concrete.AppExpression.make(data, instanceExpr, new RecursiveInstanceHoleExpression(recursiveHoleExpression == null ? data : recursiveHoleExpression.getData(), newRecursiveData), link.isExplicit());
    }

    return parameters.testGlobalInstance(instanceExpr) ? new Pair<>(instanceExpr, actualClass) : null;
  }

  @Override
  public GlobalInstancePool subst(ExprSubstitution substitution) {
    return myInstancePool != null ? new GlobalInstancePool(myInstanceProvider, myCheckTypeVisitor, myInstancePool.subst(substitution)) : this;
  }
}
