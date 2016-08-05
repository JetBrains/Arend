package com.jetbrains.jetpad.vclang.term.context.binding;

public interface Binding extends Variable {
  class Helper {
    public static String toString(Binding binding) {
      return (binding.getName() == null ? "_" : binding.getName()) + (binding.getType() == null ? "" : " : " + binding.getType());
    }
  }
}
