package org.arend.typechecking.instance.pool;

import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCFieldReferable;
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
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, TCFieldReferable fieldRef, Equations equations, Concrete.SourceNode sourceNode) {
    return getInstance(classifyingExpression, classRef, fieldRef, false);
  }

  @Override
  public LocalInstancePool subst(ExprSubstitution substitution) {
    LocalInstancePool result = new LocalInstancePool(myVisitor);
    for (InstanceData data : myPool) {
      Expression newValue = substitution.get(data.value.getBinding());
      result.myPool.add(new InstanceData(data.key == null ? null : data.key.subst(substitution), data.classRef, newValue != null && newValue.isInstance(ReferenceExpression.class) ? newValue.cast(ReferenceExpression.class) : data.value, data.sourceNode));
    }
    return result;
  }

  private Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, TCFieldReferable fieldRef, boolean isField) {
    for (int i = myPool.size() - 1; i >= 0; i--) {
      InstanceData instanceData = myPool.get(i);

      boolean ok;
      if (isField || fieldRef != null && (fieldRef.isFieldSynonym() || instanceData.classRef.isRenamed(fieldRef))) {
        ok = instanceData.classRef.isSubClassOf(classRef);
      } else {
        TCClassReferable underlyingRef1 = instanceData.classRef.getUnderlyingTypecheckable();
        TCClassReferable underlyingRef2 = classRef.getUnderlyingTypecheckable();
        ok = underlyingRef1 != null && underlyingRef2 != null && underlyingRef1.isSubClassOf(underlyingRef2);
      }

      if (ok && (instanceData.key == classifyingExpression || instanceData.key != null && instanceData.key.equals(classifyingExpression))) {
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
    Expression oldInstance = getInstance(classifyingExpression, classRef, null, true);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new InstanceData(classifyingExpression, classRef, instance, sourceNode));
      return null;
    }
  }
}
