package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;

import java.util.ArrayList;
import java.util.List;

public interface DependentLink extends Binding {
  boolean isExplicit();
  void setExplicit(boolean isExplicit);
  void setType(Expression type);
  DependentLink getNext();
  void setNext(DependentLink next);
  DependentLink subst(Substitution subst);
  DependentLink getNextTyped(List<String> names);

  class Helper {
    public static void freeSubsts(DependentLink link, Substitution substitution) {
      for (; link != null; link = link.getNext()) {
        substitution.getDomain().remove(link);
      }
    }

    public static Substitution toSubstitution(DependentLink link, List<Expression> expressions) {
      Substitution result = new Substitution();
      for (Expression expression : expressions) {
        result.addMapping(link, expression);
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

    public static DependentLink get(DependentLink link, int index) {
      for (int i = 0; i < index; i++) {
        if (link == null) {
          return null;
        }
        link = link.getNext();
      }
      return link;
    }

    public static int getIndex(DependentLink begin, DependentLink link) {
      for (int index = 0; begin != null; begin = begin.getNext(), index++) {
        if (begin == link) {
          return index;
        }
      }
      return -1;
    }
  }
}
