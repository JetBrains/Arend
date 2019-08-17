package org.arend.core.expr.type;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.ErrorReporter;

public interface Type {
  Expression getExpr();
  Sort getSortOfType();
  Type subst(SubstVisitor substVisitor);
  Type strip(ErrorReporter errorReporter);
  Type normalize(NormalizeVisitor.Mode mode);
}
