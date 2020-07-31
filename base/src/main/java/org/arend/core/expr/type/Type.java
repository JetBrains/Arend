package org.arend.core.expr.type;

import org.arend.core.expr.Expression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.ops.NormalizationMode;

public interface Type {
  Expression getExpr();
  Sort getSortOfType();
  Type subst(SubstVisitor substVisitor);
  void subst(InPlaceLevelSubstVisitor substVisitor);
  Type strip(StripVisitor visitor);
  Type normalize(NormalizeVisitor visitor, NormalizationMode mode);

  Expression OMEGA = new UniverseExpression(new Sort(Level.INFINITY, Level.INFINITY));

  default boolean isOmega() {
    return false;
  }
}
