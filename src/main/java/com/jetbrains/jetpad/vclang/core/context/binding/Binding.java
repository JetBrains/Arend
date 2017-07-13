package com.jetbrains.jetpad.vclang.core.context.binding;

import com.jetbrains.jetpad.vclang.core.expr.Expression;

public interface Binding extends Variable {
  Expression getTypeExpr();
}
