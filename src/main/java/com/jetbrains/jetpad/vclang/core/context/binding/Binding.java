package com.jetbrains.jetpad.vclang.core.context.binding;

import com.jetbrains.jetpad.vclang.core.expr.type.Type;

public interface Binding extends Variable {
  String getName();
  Type getType();

  class Helper {
    public static String toString(Binding binding) {
      Type type = binding.getType();
      return (binding.getName() == null ? "_" : binding.getName()) + (type == null ? "" : " : " + type.getExpr());
    }
  }
}
