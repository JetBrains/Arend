package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DependentLink extends Binding {
  boolean isExplicit();
  void setExplicit(boolean isExplicit);
  void setType(Expression type);
  DependentLink getNext();
  void setNext(DependentLink next);
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

    public static List<Binding> toContext(DependentLink link) {
      List<Binding> result = new ArrayList<>();
      for (; link != null; link = link.getNext()) {
        result.add(link);
      }
      return result;
    }

    public static int size(DependentLink link) {
      int result = 0;
      for (; link != null; link = link.getNext()) {
        result++;
      }
      return result;
    }
  }
}
