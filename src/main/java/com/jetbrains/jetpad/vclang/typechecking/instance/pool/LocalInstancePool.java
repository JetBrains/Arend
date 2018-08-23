package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.ErrorExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

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
