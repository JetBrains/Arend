package org.arend.typechecking.instance.pool;

import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.result.TypecheckingResult;

public interface InstancePool {
  TypecheckingResult getInstance(Expression classifyingExpression, Expression expectedType, TCClassReferable classRef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData);
  Concrete.Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData);
  InstancePool subst(ExprSubstitution substitution);
  InstancePool getLocalInstancePool();
}
