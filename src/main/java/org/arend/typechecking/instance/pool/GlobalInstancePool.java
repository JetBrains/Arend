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
      normClassifyingExpression = normClassifyingExpression.normalize(NormalizeVisitor.Mode.WHNF);
      while (normClassifyingExpression.isInstance(LamExpression.class)) {
        normClassifyingExpression = normClassifyingExpression.cast(LamExpression.class).getBody();
      }
      if (!(normClassifyingExpression.isInstance(DefCallExpression.class) || normClassifyingExpression.isInstance(SigmaExpression.class) || normClassifyingExpression.isInstance(UniverseExpression.class) || normClassifyingExpression.isInstance(IntegerExpression.class))) {
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

        Expression instanceClassifyingExpr = ((ClassCallExpression) instanceDef.getResultType()).getImplementationHere(classifyingField);
        if (instanceClassifyingExpr != null) {
          instanceClassifyingExpr = instanceClassifyingExpr.normalize(NormalizeVisitor.Mode.WHNF);
        }
        while (instanceClassifyingExpr instanceof LamExpression) {
          instanceClassifyingExpr = ((LamExpression) instanceClassifyingExpr).getBody();
        }
        return
          instanceClassifyingExpr instanceof UniverseExpression && finalClassifyingExpression.isInstance(UniverseExpression.class) ||
          instanceClassifyingExpr instanceof SigmaExpression && finalClassifyingExpression.isInstance(SigmaExpression.class) ||
          instanceClassifyingExpr instanceof IntegerExpression && (finalClassifyingExpression.isInstance(IntegerExpression.class) && ((IntegerExpression) instanceClassifyingExpr).isEqual(finalClassifyingExpression.cast(IntegerExpression.class)) ||
            finalClassifyingExpression.isInstance(ConCallExpression.class) && ((IntegerExpression) instanceClassifyingExpr).match(finalClassifyingExpression.cast(ConCallExpression.class).getDefinition())) ||
          instanceClassifyingExpr instanceof DefCallExpression && finalClassifyingExpression.isInstance(DefCallExpression.class) && ((DefCallExpression) instanceClassifyingExpr).getDefinition() == finalClassifyingExpression.cast(DefCallExpression.class).getDefinition();
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
