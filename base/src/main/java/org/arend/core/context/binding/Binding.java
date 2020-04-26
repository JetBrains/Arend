package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.ext.core.context.CoreBinding;

public interface Binding extends CoreBinding {
  @Override Expression getTypeExpr();
  void strip(StripVisitor stripVisitor);
  void subst(InPlaceLevelSubstVisitor substVisitor);

  default boolean isHidden() {
    return false;
  }
}
