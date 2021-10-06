package org.arend.core.context.param;

import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.expr.AbstractedExpression;
import org.arend.extImpl.AbstractedDependentLinkType;
import org.arend.typechecking.result.TypecheckingResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface DependentLink extends Binding, CoreParameter {
  void setExplicit(boolean isExplicit);
  void setType(Type type);
  @NotNull @Override DependentLink getNext();
  void setNext(DependentLink next);
  void setName(String name);
  DependentLink subst(SubstVisitor substVisitor, int size, boolean updateSubst);
  TypedDependentLink getNextTyped(List<String> names);

  @Override
  default Binding subst(SubstVisitor visitor) {
    return visitor.isEmpty() ? this : Helper.subst(this, visitor);
  }

  @NotNull
  @Override
  default TypecheckingResult getTypedType() {
    Type type = getType();
    return new TypecheckingResult(type.getExpr(), new UniverseExpression(type.getSortOfType()));
  }

  @NotNull
  @Override
  default CoreBinding getBinding() {
    return this;
  }

  @NotNull
  @Override
  default Expression getTypeExpr() {
    return getType().getExpr();
  }

  @Override
  default @NotNull AbstractedExpression abstractType(int size) {
    if (size >= Helper.size(this)) {
      throw new IllegalArgumentException();
    }
    return AbstractedDependentLinkType.make(this, size);
  }

  @Override
  default @NotNull CoreParameter insertParameters(@NotNull Map<CoreParameter, CoreParameter> map) {
    if (map.isEmpty()) return this;
    LinkList list = new LinkList();
    ExprSubstitution substitution = new ExprSubstitution();
    SubstVisitor visitor = new SubstVisitor(substitution, LevelSubstitution.EMPTY);
    for (DependentLink param = this; param.hasNext(); param = param.getNext()) {
      DependentLink newParam = param.subst(visitor, 1, false);
      list.append(newParam);
      substitution.add(param, new ReferenceExpression(newParam));
      CoreParameter param1 = map.get(param);
      if (param1 != null) {
        if (!(param1 instanceof DependentLink)) {
          throw new IllegalArgumentException();
        }
        DependentLink param2 = Helper.subst((DependentLink) param1, visitor);
        list.append(param2);
        substitution.add((DependentLink) param1, new ReferenceExpression(param2));
      }
    }
    return list.getFirst();
  }

  class Helper {
    public static void freeSubsts(DependentLink link, ExprSubstitution substitution) {
      for (; link.hasNext(); link = link.getNext()) {
        substitution.remove(link);
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

    public static ExprSubstitution toSubstitution(List<DependentLink> links, List<? extends Expression> expressions) {
      ExprSubstitution result = new ExprSubstitution();
      for (int i = 0; i < Math.min(links.size(), expressions.size()); ++i) {
        result.add(links.get(i), expressions.get(i));
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

    public static List<DependentLink> toList(DependentLink link) {
      List<DependentLink> result = new ArrayList<>();
      for (; link.hasNext(); link = link.getNext()) {
        result.add(link);
      }
      return result;
    }

    public static DependentLink subst(DependentLink link, ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
      return link.subst(new SubstVisitor(exprSubst, levelSubst), Integer.MAX_VALUE, false);
    }

    public static DependentLink subst(DependentLink link, SubstVisitor substVisitor) {
      return link.subst(substVisitor, Integer.MAX_VALUE, false);
    }

    public static DependentLink subst(DependentLink link, ExprSubstitution substitution, boolean updateSubst) {
      return link.subst(new SubstVisitor(substitution, LevelSubstitution.EMPTY), Integer.MAX_VALUE, updateSubst);
    }

    public static DependentLink subst(DependentLink link, ExprSubstitution substitution) {
      return subst(link, substitution, LevelSubstitution.EMPTY);
    }

    public static DependentLink copy(DependentLink link) {
      return subst(link, new ExprSubstitution(), LevelSubstitution.EMPTY);
    }

    public static SingleDependentLink subst(SingleDependentLink link, ExprSubstitution substitution) {
      return subst(link, new SubstVisitor(substitution, LevelSubstitution.EMPTY));
    }

    public static List<DependentLink> subst(List<DependentLink> links, ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
      List<DependentLink> newLinks = new ArrayList<>();
      int i = 0;
      while (i < links.size()) {
        DependentLink substLink = DependentLink.Helper.subst(links.get(i), exprSubst, levelSubst);
        while (substLink.hasNext()) {
          newLinks.add(substLink);
          substLink = substLink.getNext();
          ++i;
        }
      }
      return newLinks;
    }

    public static SingleDependentLink subst(SingleDependentLink link, SubstVisitor substVisitor) {
      return link.subst(substVisitor, Integer.MAX_VALUE, false);
    }

    public static DependentLink take(DependentLink link, int size) {
      return link.subst(new SubstVisitor(new ExprSubstitution(), LevelSubstitution.EMPTY), size, false);
    }

    public static SingleDependentLink take(SingleDependentLink link, int size) {
      return link.subst(new SubstVisitor(new ExprSubstitution(), LevelSubstitution.EMPTY), size, false);
    }
  }

  static String toString(DependentLink binding) {
    return (binding.getName() == null ? "_" : binding.getName()) + " : " + binding.getTypeExpr();
  }
}
