package org.arend.core.context.binding;

import org.arend.core.expr.Expression;

public interface Binding extends Variable {
  Expression getTypeExpr();
}
