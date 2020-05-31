package org.arend.typechecking.instance.pool;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Pair;

import java.util.ArrayList;
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
  public TypecheckingResult getInstance(Expression classifyingExpression, Expression expectedType, ClassDefinition classDef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression) {
    if (myInstancePool != null) {
      TypecheckingResult result = myInstancePool.getInstance(classifyingExpression, expectedType, classDef, sourceNode, recursiveHoleExpression);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider == null) {
      return null;
    }

    Pair<Concrete.Expression, ClassDefinition> pair = getInstancePair(classifyingExpression, classDef, sourceNode, recursiveHoleExpression);
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
  public Concrete.Expression getInstance(Expression classifyingExpression, ClassDefinition classDef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression) {
    if (myInstancePool != null) {
      Concrete.Expression result = myInstancePool.getInstance(classifyingExpression, classDef, sourceNode, recursiveHoleExpression);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider == null) {
      return null;
    }

    Pair<Concrete.Expression, ClassDefinition> pair = getInstancePair(classifyingExpression, classDef, sourceNode, recursiveHoleExpression);
    return pair == null ? null : pair.proj1;
  }

  private Pair<Concrete.Expression, ClassDefinition> getInstancePair(Expression classifyingExpression, ClassDefinition classDef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression) {
    ClassField classifyingField;
    Expression normClassifyingExpression = classifyingExpression;
    if (classifyingExpression != null) {
      normClassifyingExpression = normClassifyingExpression.normalize(NormalizationMode.WHNF).getUnderlyingExpression();
      while (normClassifyingExpression instanceof LamExpression) {
        normClassifyingExpression = ((LamExpression) normClassifyingExpression).getBody().getUnderlyingExpression();
      }
      if (!(normClassifyingExpression instanceof DefCallExpression || normClassifyingExpression instanceof SigmaExpression || normClassifyingExpression instanceof UniverseExpression || normClassifyingExpression instanceof IntegerExpression)) {
        return null;
      }

      classifyingField = classDef.getClassifyingField();
      if (classifyingField == null) {
        return null;
      }
    } else {
      classifyingField = null;
    }

    Expression finalClassifyingExpression = normClassifyingExpression;
    class MyPredicate implements Predicate<Concrete.FunctionDefinition> {
      private FunctionDefinition instanceDef = null;

      @Override
      public boolean test(Concrete.FunctionDefinition instance) {
        instanceDef = (FunctionDefinition) myCheckTypeVisitor.getTypecheckingState().getTypechecked(instance.getData());
        if (!(instanceDef != null && instanceDef.status().headerIsOK() && instanceDef.getResultType() instanceof ClassCallExpression && ((ClassCallExpression) instanceDef.getResultType()).getDefinition().isSubClassOf(classDef))) {
          return false;
        }

        if (finalClassifyingExpression == null) {
          return true;
        }

        Expression instanceClassifyingExpr = ((ClassCallExpression) instanceDef.getResultType()).getAbsImplementationHere(classifyingField);
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

    return new Pair<>(instanceExpr, actualClass);
  }

  @Override
  public GlobalInstancePool subst(ExprSubstitution substitution) {
    return myInstancePool != null ? new GlobalInstancePool(myInstanceProvider, myCheckTypeVisitor, myInstancePool.subst(substitution)) : this;
  }
}
