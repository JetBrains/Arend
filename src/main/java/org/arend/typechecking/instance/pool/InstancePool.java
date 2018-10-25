package org.arend.typechecking.instance.pool;

import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCFieldReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;

public interface InstancePool {
  Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, TCFieldReferable fieldRef, Equations equations, Concrete.SourceNode sourceNode);
  InstancePool subst(ExprSubstitution substitution);
}
