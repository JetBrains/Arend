package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.ext.core.context.CoreBinding;

public interface Binding extends Variable, CoreBinding {
  @Override Expression getTypeExpr();
  void strip(StripVisitor stripVisitor);

  default boolean isHidden() {
    return false;
  }
}
