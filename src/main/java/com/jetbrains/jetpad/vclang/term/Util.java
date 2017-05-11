package com.jetbrains.jetpad.vclang.term;

import java.util.List;

public class Util {
  public static void getReferableList(List<? extends Abstract.Argument> parameters, List<Abstract.ReferableSourceNode> result) {
    for (Abstract.Argument argument : parameters) {
      if (argument instanceof Abstract.TelescopeArgument) {
        result.addAll(((Abstract.TelescopeArgument) argument).getReferableList());
      } else
      if (argument instanceof Abstract.TypeArgument) {
        result.add(null);
      } else
      if (argument instanceof Abstract.NameArgument) {
        result.add(((Abstract.NameArgument) argument).getReferable());
      } else {
        throw new IllegalStateException();
      }
    }
  }
}
