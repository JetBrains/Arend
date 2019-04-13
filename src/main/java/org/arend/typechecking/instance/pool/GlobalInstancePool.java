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
import org.arend.typechecking.visitor.CheckTypeVisitor;

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
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Equations equations, Concrete.SourceNode sourceNode) {
    if (myInstancePool != null) {
      Expression result = myInstancePool.getInstance(classifyingExpression, classRef, equations, sourceNode);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider == null) {
      return null;
    }

    ClassField classifyingField;
    if (classifyingExpression != null) {
      classifyingExpression = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF);
      while (classifyingExpression.isInstance(LamExpression.class)) {
        classifyingExpression = classifyingExpression.cast(LamExpression.class).getBody();
      }
      if (!(classifyingExpression.isInstance(DefCallExpression.class) || classifyingExpression.isInstance(SigmaExpression.class) || classifyingExpression.isInstance(UniverseExpression.class) || classifyingExpression.isInstance(IntegerExpression.class))) {
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

    Expression normClassifyingExpression = classifyingExpression;
    class MyPredicate implements Predicate<Concrete.FunctionDefinition> {
      private FunctionDefinition instanceDef = null;

      @Override
      public boolean test(Concrete.FunctionDefinition instance) {
        instanceDef = (FunctionDefinition) myTypecheckerState.getTypechecked(instance.getData());
        if (instanceDef == null || !instanceDef.status().headerIsOK() || !(instanceDef.getResultType() instanceof ClassCallExpression)) {
          return false;
        }

        if (normClassifyingExpression == null) {
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
          instanceClassifyingExpr instanceof UniverseExpression && normClassifyingExpression.isInstance(UniverseExpression.class) ||
          instanceClassifyingExpr instanceof SigmaExpression && normClassifyingExpression.isInstance(SigmaExpression.class) ||
          instanceClassifyingExpr instanceof IntegerExpression && (normClassifyingExpression.isInstance(IntegerExpression.class) && ((IntegerExpression) instanceClassifyingExpr).isEqual(normClassifyingExpression.cast(IntegerExpression.class)) ||
            normClassifyingExpression.isInstance(ConCallExpression.class) && ((IntegerExpression) instanceClassifyingExpr).match(normClassifyingExpression.cast(ConCallExpression.class).getDefinition())) ||
          instanceClassifyingExpr instanceof DefCallExpression && normClassifyingExpression.isInstance(DefCallExpression.class) && ((DefCallExpression) instanceClassifyingExpr).getDefinition() == normClassifyingExpression.cast(DefCallExpression.class).getDefinition();
      }
    }

    MyPredicate predicate = new MyPredicate();
    Concrete.FunctionDefinition instance = myInstanceProvider.findInstance(classRef, predicate);
    if (instance == null || predicate.instanceDef == null) {
      return null;
    }

    Concrete.Expression instanceExpr = new Concrete.ReferenceExpression(sourceNode.getData(), instance.getData());
    for (DependentLink link = predicate.instanceDef.getParameters(); link.hasNext(); link = link.getNext()) {
      if (link.isExplicit()) {
        instanceExpr = Concrete.AppExpression.make(sourceNode.getData(), instanceExpr, new Concrete.HoleExpression(sourceNode.getData()), true);
      }
    }

    ClassDefinition classDef = ((ClassCallExpression) predicate.instanceDef.getResultType()).getDefinition();
    Expression expectedType = classifyingField == null ? null : myCheckTypeVisitor.fixClassExtSort(new ClassCallExpression(classDef, Sort.generateInferVars(myCheckTypeVisitor.getEquations(), classDef.hasUniverses(), sourceNode)), sourceNode);
    CheckTypeVisitor.Result result = myCheckTypeVisitor.checkExpr(instanceExpr, expectedType);
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
