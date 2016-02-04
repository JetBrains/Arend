package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public interface Binding {
  String getName();
  Expression getType();
  boolean isInference();

  class Helper {
    public static String toString(Binding binding) {
      return (binding.getName() == null ? "_" : binding.getName()) + (binding.getType() == null ? "" : " : " + binding.getType());
    }
  }
}
