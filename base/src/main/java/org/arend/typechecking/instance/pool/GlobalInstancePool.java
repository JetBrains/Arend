package org.arend.typechecking.instance.pool;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.instance.InstanceSearchParameters;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class GlobalInstancePool implements InstancePool {
  private final InstanceProvider myInstanceProvider;
  private final CheckTypeVisitor myCheckTypeVisitor;
  private InstancePool myInstancePool;

  public GlobalInstancePool(InstanceProvider instanceProvider, CheckTypeVisitor checkTypeVisitor) {
    myInstanceProvider = instanceProvider;
    myCheckTypeVisitor = checkTypeVisitor;
  }

  public GlobalInstancePool(InstanceProvider instanceProvider, CheckTypeVisitor checkTypeVisitor, InstancePool instancePool) {
    myInstanceProvider = instanceProvider;
    myCheckTypeVisitor = checkTypeVisitor;
    myInstancePool = instancePool;
  }

  public InstancePool getInstancePool() {
    return myInstancePool;
  }

  public void setInstancePool(InstancePool instancePool) {
    myInstancePool = instancePool;
  }

  public InstanceProvider getInstanceProvider() {
    return myInstanceProvider;
  }

  @Override
  public InstancePool getLocalInstancePool() {
    return myInstancePool.getLocalInstancePool();
  }

  @Override
  public Expression addLocalInstance(Expression classifyingExpression, ClassDefinition classDef, Expression instance, Concrete.SourceNode sourceNode) {
    return myInstancePool.addLocalInstance(classifyingExpression, classDef, instance, sourceNode);
  }

  @Override
  public List<?> getLocalInstances() {
    return myInstancePool == null ? Collections.emptyList() : myInstancePool.getLocalInstances();
  }

  @Override
  public TypecheckingResult getInstance(Expression classifyingExpression, Expression expectedType, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression) {
    if (myInstancePool != null) {
      TypecheckingResult result = myInstancePool.getInstance(classifyingExpression, expectedType, parameters, sourceNode, recursiveHoleExpression);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider == null) {
      return null;
    }

    Pair<Concrete.Expression, ClassDefinition> pair = getInstancePair(classifyingExpression, parameters, sourceNode, recursiveHoleExpression);
    if (pair == null) {
      return null;
    }

    if (expectedType == null) {
      ClassCallExpression classCall = classifyingExpression == null ? null : new ClassCallExpression(pair.proj2, Sort.generateInferVars(myCheckTypeVisitor.getEquations(), pair.proj2.getUniverseKind(), sourceNode));
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

  @Override
  public Concrete.Expression getInstance(Expression classifyingExpression, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression) {
    if (myInstancePool != null) {
      Concrete.Expression result = myInstancePool.getInstance(classifyingExpression, parameters, sourceNode, recursiveHoleExpression);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider == null) {
      return null;
    }

    Pair<Concrete.Expression, ClassDefinition> pair = getInstancePair(classifyingExpression, parameters, sourceNode, recursiveHoleExpression);
    return pair == null ? null : pair.proj1;
  }

  private Pair<Concrete.Expression, ClassDefinition> getInstancePair(Expression classifyingExpression, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression) {
    if (!parameters.searchGlobal()) {
      return null;
    }

    Expression normClassifyingExpression = classifyingExpression;
    if (classifyingExpression != null) {
      normClassifyingExpression = normClassifyingExpression.normalize(NormalizationMode.WHNF);
      while (normClassifyingExpression instanceof LamExpression) {
        normClassifyingExpression = ((LamExpression) normClassifyingExpression).getBody().normalize(NormalizationMode.WHNF);
      }
      if (!(normClassifyingExpression instanceof DefCallExpression || normClassifyingExpression instanceof SigmaExpression || normClassifyingExpression instanceof UniverseExpression || normClassifyingExpression instanceof IntegerExpression)) {
        return null;
      }
    }

    Expression finalClassifyingExpression = normClassifyingExpression;
    class MyPredicate implements Predicate<Concrete.FunctionDefinition> {
      private FunctionDefinition instanceDef = null;

      @Override
      public boolean test(Concrete.FunctionDefinition instance) {
        instanceDef = (FunctionDefinition) myCheckTypeVisitor.getTypecheckingState().getTypechecked(instance.getData());
        if (!(instanceDef != null && instanceDef.status().headerIsOK() && instanceDef.getResultType() instanceof ClassCallExpression && parameters.testClass(((ClassCallExpression) instanceDef.getResultType()).getDefinition()) && parameters.testGlobalInstance(instanceDef))) {
          return false;
        }

        ClassCallExpression classCall = (ClassCallExpression) instanceDef.getResultType();
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
        return
          instanceClassifyingExpr instanceof UniverseExpression && finalClassifyingExpression instanceof UniverseExpression ||
            instanceClassifyingExpr instanceof SigmaExpression && finalClassifyingExpression instanceof SigmaExpression ||
            instanceClassifyingExpr instanceof IntegerExpression && (finalClassifyingExpression instanceof IntegerExpression && ((IntegerExpression) instanceClassifyingExpr).isEqual((IntegerExpression) finalClassifyingExpression) ||
              finalClassifyingExpression instanceof ConCallExpression && ((IntegerExpression) instanceClassifyingExpr).match(((ConCallExpression) finalClassifyingExpression).getDefinition())) ||
            instanceClassifyingExpr instanceof DefCallExpression && finalClassifyingExpression instanceof DefCallExpression && ((DefCallExpression) instanceClassifyingExpr).getDefinition() == ((DefCallExpression) finalClassifyingExpression).getDefinition();
      }
    }

    MyPredicate predicate = new MyPredicate();
    Concrete.FunctionDefinition instance = myInstanceProvider.findInstance(predicate);
    if (instance == null || predicate.instanceDef == null) {
      return null;
    }

    ClassDefinition actualClass = ((ClassCallExpression) predicate.instanceDef.getResultType()).getDefinition();
    Concrete.Expression instanceExpr = new Concrete.ReferenceExpression(sourceNode.getData(), instance.getData());
    for (DependentLink link = predicate.instanceDef.getParameters(); link.hasNext(); link = link.getNext()) {
      List<RecursiveInstanceData> newRecursiveData = new ArrayList<>((recursiveHoleExpression == null ? 0 : recursiveHoleExpression.recursiveData.size()) + 1);
      if (recursiveHoleExpression != null) {
        newRecursiveData.addAll(recursiveHoleExpression.recursiveData);
      }
      newRecursiveData.add(new RecursiveInstanceData(instance.getData(), actualClass.getReferable(), classifyingExpression));
      instanceExpr = Concrete.AppExpression.make(sourceNode.getData(), instanceExpr, new RecursiveInstanceHoleExpression(recursiveHoleExpression == null ? sourceNode : recursiveHoleExpression.getData(), newRecursiveData), link.isExplicit());
    }

    return parameters.testGlobalInstance(instanceExpr) ? new Pair<>(instanceExpr, actualClass) : null;
  }

  @Override
  public GlobalInstancePool subst(ExprSubstitution substitution) {
    return myInstancePool != null ? new GlobalInstancePool(myInstanceProvider, myCheckTypeVisitor, myInstancePool.subst(substitution)) : this;
  }
}
