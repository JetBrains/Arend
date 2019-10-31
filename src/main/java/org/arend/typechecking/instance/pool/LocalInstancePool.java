package org.arend.typechecking.instance.pool;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements InstancePool {
  static private class InstanceData {
    final Expression key;
    final ExpectedType keyType;
    final TCClassReferable classRef;
    final Expression value;
    final Concrete.SourceNode sourceNode;

    InstanceData(Expression key, ExpectedType keyType, TCClassReferable classRef, Expression value, Concrete.SourceNode sourceNode) {
      this.key = key;
      this.keyType = keyType;
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
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Equations equations, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData) {
    return getInstance(classifyingExpression, classRef);
  }

  @Override
  public LocalInstancePool subst(ExprSubstitution substitution) {
    LocalInstancePool result = new LocalInstancePool(myVisitor);
    for (InstanceData data : myPool) {
      Expression newValue = data.value instanceof ReferenceExpression ? substitution.get(((ReferenceExpression) data.value).getBinding()) : null;
      newValue = newValue == null ? null : newValue.cast(ReferenceExpression.class);
      result.myPool.add(new InstanceData(data.key == null ? null : data.key.subst(substitution), data.keyType == null ? null : data.keyType.subst(substitution), data.classRef, newValue == null ? data.value : newValue, data.sourceNode));
    }
    return result;
  }

  @Override
  public InstancePool getLocalInstancePool() {
    return this;
  }

  private Expression getInstance(Expression classifyingExpression, TCClassReferable classRef) {
    for (int i = myPool.size() - 1; i >= 0; i--) {
      InstanceData instanceData = myPool.get(i);
      if (instanceData.classRef.isSubClassOf(classRef) && (instanceData.key == classifyingExpression || instanceData.key != null && classifyingExpression != null && Expression.compare(instanceData.key, classifyingExpression, instanceData.keyType, Equations.CMP.EQ))) {
        return instanceData.value;
      }
    }
    return null;
  }

  public Expression addInstance(Expression classifyingExpression, ExpectedType classifyingExpressionType, TCClassReferable classRef, Expression instance, Concrete.SourceNode sourceNode) {
    Expression oldInstance = getInstance(classifyingExpression, classRef);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new InstanceData(classifyingExpression, classifyingExpressionType, classRef, instance, sourceNode));
      return null;
    }
  }
}
