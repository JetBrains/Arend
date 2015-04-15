package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public abstract class Binding {
  private final String myName;

  public Binding(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  public abstract Expression getType();
}
