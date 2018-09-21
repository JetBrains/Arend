package org.arend.typechecking.instance.pool;

import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.error.local.TypeMismatchError;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements InstancePool {
  static private class InstanceData {
    final Expression key;
    final TCClassReferable classRef;
    final ReferenceExpression value;
    final Concrete.SourceNode sourceNode;

    InstanceData(Expression key, TCClassReferable classRef, ReferenceExpression value, Concrete.SourceNode sourceNode) {
      this.key = key;
      this.classRef = classRef;
      this.value = value;
      this.sourceNode = sourceNode;
    }
  }

  private final List<InstanceData> myPool = new ArrayList<>();
  private final CheckTypeVisitor myVisitor;

  public LocalInstancePool(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, boolean isField, Equations equations, Concrete.SourceNode sourceNode) {
    return getInstance(classifyingExpression, classRef, isField);
  }

  @Override
  public LocalInstancePool subst(ExprSubstitution substitution) {
    LocalInstancePool result = new LocalInstancePool(myVisitor);
    for (InstanceData data : myPool) {
      Expression newValue = substitution.get(data.value.getBinding());
      result.myPool.add(newValue != null && newValue.isInstance(ReferenceExpression.class) ? new InstanceData(data.key, data.classRef, newValue.cast(ReferenceExpression.class), data.sourceNode) : data);
    }
    return result;
  }

  private Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, boolean isField) {
    for (int i = myPool.size() - 1; i >= 0; i--) {
      InstanceData instanceData = myPool.get(i);
      if ((isField ? instanceData.classRef.isSubClassOf(classRef) : instanceData.classRef.getUnderlyingTypecheckable().isSubClassOf(classRef.getUnderlyingTypecheckable())) && (instanceData.key == classifyingExpression || instanceData.key != null && instanceData.key.equals(classifyingExpression))) {
        Expression result = instanceData.value;
        if (instanceData.key == classifyingExpression) {
          return result;
        }
        if (!CompareVisitor.compare(myVisitor.getEquations(), Equations.CMP.LE, instanceData.key, classifyingExpression, instanceData.sourceNode)) {
          LocalError error = new TypeMismatchError(instanceData.key, classifyingExpression, instanceData.sourceNode);
          myVisitor.getErrorReporter().report(error);
          result = new ErrorExpression(result, error);
        }
        return result;
      }
    }
    return null;
  }

  public Expression addInstance(Expression classifyingExpression, TCClassReferable classRef, ReferenceExpression instance, Concrete.SourceNode sourceNode) {
    Expression oldInstance = getInstance(classifyingExpression, classRef, true);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new InstanceData(classifyingExpression, classRef, instance, sourceNode));
      return null;
    }
  }
}
