package org.arend.typechecking.instance.pool;

import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.result.TypecheckingResult;

public class EmptyInstancePool implements InstancePool {
  public static EmptyInstancePool INSTANCE = new EmptyInstancePool();

  private EmptyInstancePool() {}

  @Override
  public TypecheckingResult getInstance(Expression classifyingExpression, Expression expectedType, TCClassReferable classRef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData) {
    return null;
  }

  @Override
  public Concrete.Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData) {
    return null;
  }

  @Override
  public EmptyInstancePool subst(ExprSubstitution substitution) {
    return this;
  }

  @Override
  public InstancePool getLocalInstancePool() {
    return this;
  }
}
