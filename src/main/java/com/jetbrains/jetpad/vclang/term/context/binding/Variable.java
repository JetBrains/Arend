package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public interface Variable extends Callable {
  String getName();
  Expression getType();
}
