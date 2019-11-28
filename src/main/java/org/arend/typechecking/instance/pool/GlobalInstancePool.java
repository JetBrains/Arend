package org.arend.typechecking.instance.pool;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class GlobalInstancePool implements InstancePool {
  private final TypecheckerState myTypecheckerState;
  private final InstanceProvider myInstanceProvider;
  private final CheckTypeVisitor myCheckTypeVisitor;
  private InstancePool myInstancePool;

  public GlobalInstancePool(TypecheckerState typecheckerState, InstanceProvider instanceProvider, CheckTypeVisitor checkTypeVisitor) {
    myTypecheckerState = typecheckerState;
    myInstanceProvider = instanceProvider;
    myCheckTypeVisitor = checkTypeVisitor;
  }

  public InstancePool getInstancePool() {
    return myInstancePool;
  }

  public void setInstancePool(InstancePool instancePool) {
    myInstancePool = instancePool;
  }

  @Override
  public InstancePool getLocalInstancePool() {
    return myInstancePool.getLocalInstancePool();
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Equations equations, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveHoleExpression) {
    if (myInstancePool != null) {
      Expression result = myInstancePool.getInstance(classifyingExpression, classRef, equations, sourceNode, recursiveHoleExpression);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider == null) {
      return null;
    }

    ClassField classifyingField;
    Expression normClassifyingExpression = classifyingExpression;
    if (normClassifyingExpression != null) {
      normClassifyingExpression = normClassifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).getUnderlyingExpression();
      while (normClassifyingExpression instanceof LamExpression) {
        normClassifyingExpression = ((LamExpression) normClassifyingExpression).getBody().getUnderlyingExpression();
      }
      if (!(normClassifyingExpression instanceof DefCallExpression || normClassifyingExpression instanceof SigmaExpression || normClassifyingExpression instanceof UniverseExpression || normClassifyingExpression instanceof IntegerExpression)) {
        return null;
      }

      Definition typechecked = myTypecheckerState.getTypechecked(classRef);
      if (!(typechecked instanceof ClassDefinition)) {
        return null;
      }
      classifyingField = ((ClassDefinition) typechecked).getClassifyingField();
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
        instanceDef = (FunctionDefinition) myTypecheckerState.getTypechecked(instance.getData());
        if (instanceDef == null || !instanceDef.status().headerIsOK() || !(instanceDef.getResultType() instanceof ClassCallExpression)) {
          return false;
        }

        if (finalClassifyingExpression == null) {
          return true;
        }

        List<Expression> args = new ArrayList<>();
        for (DependentLink link = instanceDef.getParameters(); link.hasNext(); link = link.getNext()) {
          args.add(new ReferenceExpression(link));
        }
        Expression instanceClassifyingExpr = ((ClassCallExpression) instanceDef.getResultType()).getImplementationHere(classifyingField, new FunCallExpression(instanceDef, Sort.STD, args));
        if (instanceClassifyingExpr != null) {
          instanceClassifyingExpr = instanceClassifyingExpr.normalize(NormalizeVisitor.Mode.WHNF);
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
    Concrete.FunctionDefinition instance = myInstanceProvider.findInstance(classRef, predicate);
    if (instance == null || predicate.instanceDef == null) {
      return null;
    }

    ClassDefinition classDef = ((ClassCallExpression) predicate.instanceDef.getResultType()).getDefinition();
    Concrete.Expression instanceExpr = new Concrete.ReferenceExpression(sourceNode.getData(), instance.getData());
    for (DependentLink link = predicate.instanceDef.getParameters(); link.hasNext(); link = link.getNext()) {
      List<RecursiveInstanceData> newRecursiveData = new ArrayList<>((recursiveHoleExpression == null ? 0 : recursiveHoleExpression.recursiveData.size()) + 1);
      if (recursiveHoleExpression != null) {
        newRecursiveData.addAll(recursiveHoleExpression.recursiveData);
      }
      newRecursiveData.add(new RecursiveInstanceData(instance.getData(), classDef.getReferable(), classifyingExpression));
      instanceExpr = Concrete.AppExpression.make(sourceNode.getData(), instanceExpr, new RecursiveInstanceHoleExpression(recursiveHoleExpression == null ? sourceNode : recursiveHoleExpression.getData(), newRecursiveData), link.isExplicit());
    }

    Expression expectedType = classifyingField == null ? null : myCheckTypeVisitor.fixClassExtSort(new ClassCallExpression(classDef, Sort.generateInferVars(myCheckTypeVisitor.getEquations(), classDef.hasUniverses(), sourceNode)), sourceNode);
    TypecheckingResult result = myCheckTypeVisitor.checkExpr(instanceExpr, expectedType);
    return result == null ? new ErrorExpression(null, null) : result.expression;
  }

  @Override
  public GlobalInstancePool subst(ExprSubstitution substitution) {
    if (myInstancePool != null) {
      GlobalInstancePool result = new GlobalInstancePool(myTypecheckerState, myInstanceProvider, myCheckTypeVisitor);
      result.setInstancePool(myInstancePool.subst(substitution));
      return result;
    } else {
      return this;
    }
  }
}
