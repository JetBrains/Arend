package org.arend.core.expr.type;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.ops.NormalizationMode;

public interface Type {
  Expression getExpr();
  Sort getSortOfType();
  Type subst(SubstVisitor substVisitor);
  Type strip(StripVisitor visitor);
  Type normalize(NormalizationMode mode);
}
