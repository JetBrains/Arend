package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.Map;

public class NonDependentLink implements DependentLink {
  private final Expression myType;
  private final DependentLink myNext;

  public NonDependentLink(Expression type, DependentLink next) {
    assert next != null;
    myType = type;
    myNext = next;
  }

  @Override
  public boolean isExplicit() {
    return true;
  }

  @Override
  public DependentLink getNext() {
    return myNext;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Expression getType() {
    return myType;
  }

  @Override
  public boolean isInference() {
    return false;
  }

  @Override
  public NonDependentLink subst(Map<Binding, Expression> substs) {
    return new NonDependentLink(myType.subst(substs), myNext.subst(substs));
  }
}
