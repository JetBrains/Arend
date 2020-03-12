package org.arend.typechecking.instance.pool;

import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements InstancePool {
  static private class InstanceData {
    final Expression key;
    final Expression keyType;
    final TCClassReferable classRef;
    final Expression value;
    final Concrete.SourceNode sourceNode;

    InstanceData(Expression key, Expression keyType, TCClassReferable classRef, Expression value, Concrete.SourceNode sourceNode) {
      this.key = key;
      this.keyType = keyType;
      this.classRef = classRef;
      this.value = value;
      this.sourceNode = sourceNode;
    }
  }

  private final CheckTypeVisitor myTypechecker;
  private final List<InstanceData> myPool = new ArrayList<>();

  public LocalInstancePool(CheckTypeVisitor typechecker) {
    myTypechecker = typechecker;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, Expression expectedType, TCClassReferable classRef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData) {
    Expression result = getInstance(classifyingExpression, classRef);
    if (result == null || expectedType == null) {
      return result;
    }

    Expression actualType = result.getType();
    if (actualType == null) {
      TypecheckingError error = new TypecheckingError("Cannot infer the type of the instance", sourceNode);
      myTypechecker.getErrorReporter().report(error);
      return new ErrorExpression(error);
    }

    if (!CompareVisitor.compare(myTypechecker.getEquations(), CMP.LE, actualType, expectedType, Type.OMEGA, sourceNode)) {
      TypecheckingError error = new TypeMismatchError(expectedType, actualType, sourceNode);
      myTypechecker.getErrorReporter().report(error);
      return new ErrorExpression(error);
    }

    return result;
  }

  @Override
  public LocalInstancePool subst(ExprSubstitution substitution) {
    LocalInstancePool result = new LocalInstancePool(myTypechecker);
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
      if (instanceData.classRef.isSubClassOf(classRef) && (instanceData.key == classifyingExpression || instanceData.key != null && classifyingExpression != null && Expression.compare(instanceData.key, classifyingExpression, instanceData.keyType, CMP.EQ))) {
        return instanceData.value;
      }
    }
    return null;
  }

  public Expression addInstance(Expression classifyingExpression, Expression classifyingExpressionType, TCClassReferable classRef, Expression instance, Concrete.SourceNode sourceNode) {
    Expression oldInstance = getInstance(classifyingExpression, classRef);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new InstanceData(classifyingExpression, classifyingExpressionType, classRef, instance, sourceNode));
      return null;
    }
  }
}
