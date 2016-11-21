package com.jetbrains.jetpad.vclang.term.context.binding;

import java.util.ArrayList;
import java.util.List;

public interface Binding extends Variable {
  class Helper {
    public static String toString(Binding binding) {
      return (binding.getName() == null ? "_" : binding.getName()) + (binding.getType() == null ? "" : " : " + binding.getType());
    }

    public static List<Integer> getSublistOfUserBindings(List<? extends Binding> bindings) {
      List<Integer> userBindings = new ArrayList<>();
      for (int i = 0; i < bindings.size(); ++i) {
        if (!bindings.get(i).getName().startsWith("\\")) {
          userBindings.add(i);
        }
      }
      return userBindings;
    }
  }
}
