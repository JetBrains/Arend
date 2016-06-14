package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.LevelSubstVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.SubstVisitor;

import java.util.ArrayList;
import java.util.List;

public interface DependentLink extends Binding {
  boolean isExplicit();
  void setExplicit(boolean isExplicit);
  void setType(Expression type);
  DependentLink getNext();
  void setNext(DependentLink next);
  void setName(String name);
  DependentLink subst(ExprSubstitution subst, int size);
  DependentLink getNextTyped(List<String> names);
  boolean hasNext();

  class Helper {
    public static void freeSubsts(DependentLink link, ExprSubstitution substitution) {
      for (; link.hasNext(); link = link.getNext()) {
        substitution.getDomain().remove(link);
      }
    }

    public static ExprSubstitution toSubstitution(DependentLink link, List<? extends Expression> expressions) {
      ExprSubstitution result = new ExprSubstitution();
      for (Expression expression : expressions) {
        result.add(link, expression);
        link = link.getNext();
      }
      return result;
    }

    public static List<Binding> toContext(DependentLink link) {
      List<Binding> result = new ArrayList<>();
      for (; link.hasNext(); link = link.getNext()) {
        result.add(link);
      }
      return result;
    }

    public static List<String> toNames(DependentLink link) {
      List<String> result = new ArrayList<>();
      for (; link.hasNext(); link = link.getNext()) {
        result.add(link.getName());
      }
      return result;
    }

    public static int size(DependentLink link) {
      int result = 0;
      for (; link.hasNext(); link = link.getNext()) {
        result++;
      }
      return result;
    }

    public static DependentLink get(DependentLink link, int index) {
      for (int i = 0; i < index; i++) {
        if (!link.hasNext()) {
          return EmptyDependentLink.getInstance();
        }
        link = link.getNext();
      }
      return link;
    }

    public static DependentLink getLast(DependentLink link) {
      DependentLink last = link;
      for (; link.hasNext(); link = link.getNext()) {
        last = link;
      }
      return last;
    }

    public static int getIndex(DependentLink begin, DependentLink link) {
      for (int index = 0; begin.hasNext(); begin = begin.getNext(), index++) {
        if (begin == link) {
          return index;
        }
      }
      return -1;
    }

    public static List<DependentLink> toList(DependentLink link) {
      List<DependentLink> result = new ArrayList<>();
      for (; link.hasNext(); link = link.getNext()) {
        result.add(link);
      }
      return result;
    }

    public static DependentLink subst(DependentLink link, ExprSubstitution substitution) {
      return link.subst(substitution, Integer.MAX_VALUE);
    }

    public static DependentLink subst(DependentLink link, LevelSubstitution substitution) {
      DependentLink newParams = DependentLink.Helper.clone(link);
      for (DependentLink param = newParams; param.hasNext(); param = param.getNext()) {
        param.getType().accept(new LevelSubstVisitor(substitution), null);
      }
      return newParams;
    }

    public static <P> DependentLink accept(DependentLink link, ExprSubstitution substitution, ExpressionVisitor<? super P, ? extends Expression> visitor, P params) {
      link = DependentLink.Helper.subst(link, substitution);
      for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
        link1 = link1.getNextTyped(null);
        link1.setType(link1.getType().accept(visitor, params));
      }
      return link;
    }

    public static <P> DependentLink accept(DependentLink link, ExpressionVisitor<? super P, ? extends Expression> visitor, P params) {
      return accept(link, new ExprSubstitution(), visitor, params);
    }

    public static DependentLink clone(DependentLink link) {
      return accept(link, new SubstVisitor(new ExprSubstitution()), null);
    }
  }
}
