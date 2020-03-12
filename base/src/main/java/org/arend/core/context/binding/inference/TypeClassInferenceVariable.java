package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.ext.error.LocalError;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;

import java.util.Set;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final TCClassReferable myClassRef;
  private final boolean myOnlyLocal;
  private Expression myClassifyingExpression;
  private final RecursiveInstanceHoleExpression myRecursiveInstanceHoleExpression;

  public TypeClassInferenceVariable(String name, Expression type, TCClassReferable classRef, boolean onlyLocal, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveInstanceHoleExpression, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myClassRef = classRef;
    myOnlyLocal = onlyLocal;
    myRecursiveInstanceHoleExpression = recursiveInstanceHoleExpression;
  }

  public TCClassReferable getClassReferable() {
    return myClassRef;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return candidates.length == 0 ? new InstanceInferenceError(myClassRef, myClassifyingExpression, getSourceNode(), myRecursiveInstanceHoleExpression) : new InstanceInferenceError(myClassRef, getSourceNode(), myRecursiveInstanceHoleExpression, candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new InstanceInferenceError(myClassRef, expectedType, actualType, getSourceNode(), candidate, myRecursiveInstanceHoleExpression);
  }

  public Expression getInstance(InstancePool pool, Expression classifyingExpression, Expression expectedType, Concrete.SourceNode sourceNode) {
    Expression result = (myOnlyLocal ? pool.getLocalInstancePool() : pool).getInstance(classifyingExpression, expectedType, myClassRef, sourceNode, myRecursiveInstanceHoleExpression);
    if (result == null && myClassifyingExpression == null) {
      myClassifyingExpression = classifyingExpression;
    }
    return result;
  }
}
