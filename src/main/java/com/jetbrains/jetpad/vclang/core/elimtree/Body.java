package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.List;

public interface Body {
  boolean isWHNF(List<? extends Expression> arguments);
  Expression getStuckExpression(List<? extends Expression> arguments, Expression expression);
}
