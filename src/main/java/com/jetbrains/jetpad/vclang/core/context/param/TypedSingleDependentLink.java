package com.jetbrains.jetpad.vclang.core.context.param;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.List;

public class TypedSingleDependentLink extends TypedDependentLink implements SingleDependentLink {
  public TypedSingleDependentLink(boolean isExplicit, String name, Expression type) {
    super(isExplicit, name, type, EmptyDependentLink.getInstance());
  }

  @Override
  public TypedSingleDependentLink getNextTyped(List<String> names) {
    if (names != null) {
      names.add(getName());
    }
    return this;
  }

  @Override
  public SingleDependentLink subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, int size) {
    if (size > 0) {
      TypedSingleDependentLink result = new TypedSingleDependentLink(isExplicit(), getName(), getType().subst(exprSubst, levelSubst));
      exprSubst.add(this, new ReferenceExpression(result));
      return result;
    } else {
      return EmptyDependentLink.getInstance();
    }
  }

  @Override
  public EmptyDependentLink getNext() {
    return EmptyDependentLink.getInstance();
  }

  @Override
  public void setNext(DependentLink next) {
    if (next.hasNext()) {
      throw new IllegalStateException();
    }
  }
}
