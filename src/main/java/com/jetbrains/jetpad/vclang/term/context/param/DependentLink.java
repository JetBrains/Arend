package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DependentLink extends Binding {
  boolean isExplicit();
  void setExplicit(boolean isExplicit);
  DependentLink getNext();
  DependentLink subst(Map<Binding, Expression> substs);
  DependentLink getNextTyped(List<String> names);

  class Helper {
    public static void freeSubsts(DependentLink link, Map<Binding, Expression> substs) {
      for (; link != null; link = link.getNext()) {
        substs.remove(link);
      }
    }

    public static Map<Binding, Expression> toSubsts(DependentLink link, List<Expression> expressions) {
      Map<Binding, Expression> result = new HashMap<>();
      for (Expression expression : expressions) {
        result.put(link, expression);
        link = link.getNext();
      }
      return result;
    }
  }
}
