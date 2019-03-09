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
import java.util.Objects;

public class LocalInstancePool implements InstancePool {
  static private class InstanceData {
    final Expression key;
    final TCClassReferable classRef;
    final Expression value;
    final Concrete.SourceNode sourceNode;

    InstanceData(Expression key, TCClassReferable classRef, Expression value, Concrete.SourceNode sourceNode) {
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
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Equations equations, Concrete.SourceNode sourceNode) {
    return getInstance(classifyingExpression, classRef);
  }

  @Override
  public LocalInstancePool subst(ExprSubstitution substitution) {
    LocalInstancePool result = new LocalInstancePool(myVisitor);
    for (InstanceData data : myPool) {
      Expression newValue = data.value instanceof ReferenceExpression ? substitution.get(((ReferenceExpression) data.value).getBinding()) : null;
      result.myPool.add(new InstanceData(data.key == null ? null : data.key.subst(substitution), data.classRef, newValue != null && newValue.isInstance(ReferenceExpression.class) ? newValue.cast(ReferenceExpression.class) : data.value, data.sourceNode));
    }
    return result;
  }

  private Expression getInstance(Expression classifyingExpression, TCClassReferable classRef) {
    for (int i = myPool.size() - 1; i >= 0; i--) {
      InstanceData instanceData = myPool.get(i);
      if (instanceData.classRef.isSubClassOf(classRef) && Objects.equals(instanceData.key, classifyingExpression)) {
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

  public Expression addInstance(Expression classifyingExpression, TCClassReferable classRef, Expression instance, Concrete.SourceNode sourceNode) {
    Expression oldInstance = getInstance(classifyingExpression, classRef);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new InstanceData(classifyingExpression, classRef, instance, sourceNode));
      return null;
    }
  }
}
