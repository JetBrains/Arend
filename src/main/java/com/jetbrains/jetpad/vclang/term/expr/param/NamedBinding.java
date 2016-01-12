package com.jetbrains.jetpad.vclang.term.expr.param;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public abstract class NamedBinding implements Binding {
  private final String myName;

  public NamedBinding(String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }

  public abstract Expression getType();

  @Override
  public boolean isInference() {
    return false;
  }
}
