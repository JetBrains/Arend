package com.jetbrains.jetpad.vclang.term.expr.param;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public interface Binding {
  String getName();
  Expression getType();
  boolean isInference();
}
