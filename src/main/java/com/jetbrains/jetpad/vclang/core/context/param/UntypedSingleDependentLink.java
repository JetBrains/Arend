package com.jetbrains.jetpad.vclang.core.context.param;

import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.List;

public class UntypedSingleDependentLink extends UntypedDependentLink implements SingleDependentLink {
  public UntypedSingleDependentLink(String name, SingleDependentLink next) {
    super(name, next);
  }

  public UntypedSingleDependentLink(String name) {
    super(name);
  }

  @Override
  public SingleDependentLink getNext() {
    return (SingleDependentLink) myNext;
  }

  @Override
  public TypedSingleDependentLink getNextTyped(List<String> names) {
    return (TypedSingleDependentLink) super.getNextTyped(names);
  }

  @Override
  public SingleDependentLink subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, int size) {
    if (size == 1) {
      TypedSingleDependentLink result = new TypedSingleDependentLink(isExplicit(), getName(), getType());
      exprSubst.add(this, new ReferenceExpression(result));
      return result;
    } else
    if (size > 0) {
      UntypedSingleDependentLink result = new UntypedSingleDependentLink(getName());
      exprSubst.add(this, new ReferenceExpression(result));
      result.myNext = myNext.subst(exprSubst, levelSubst, size - 1);
      return result;
    } else {
      return EmptyDependentLink.getInstance();
    }
  }
}
