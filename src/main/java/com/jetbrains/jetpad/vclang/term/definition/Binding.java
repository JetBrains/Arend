package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public abstract class Binding implements Abstract.Binding {
  private final String myName;

  public Binding(String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }

  public abstract Expression getType();

  public abstract Binding lift(int on);
}
