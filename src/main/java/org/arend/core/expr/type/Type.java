package org.arend.core.expr.type;

import org.arend.core.expr.Expression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.jetbrains.annotations.NotNull;

public interface Type {
  Expression getExpr();
  Sort getSortOfType();
  Type subst(@NotNull SubstVisitor substVisitor);
  void subst(@NotNull InPlaceLevelSubstVisitor substVisitor);
  Type strip(@NotNull StripVisitor visitor);
  Type normalize(NormalizationMode mode);

  Expression OMEGA = new UniverseExpression(new Sort(Level.INFINITY, Level.INFINITY));

  default boolean isOmega() {
    return false;
  }
}
