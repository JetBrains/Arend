package org.arend.typechecking.instance.pool;

import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;

public interface InstancePool {
  Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData);
  InstancePool subst(ExprSubstitution substitution);
  InstancePool getLocalInstancePool();
}
