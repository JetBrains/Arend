package org.arend.core.expr.type;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.typechecking.error.LocalErrorReporter;

public interface Type {
  Expression getExpr();
  Sort getSortOfType();
  Type subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution);
  Type strip(LocalErrorReporter errorReporter);
  Type normalize(NormalizeVisitor.Mode mode);
}
