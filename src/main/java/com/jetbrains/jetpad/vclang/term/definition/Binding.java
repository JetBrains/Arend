package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;

public abstract class Binding implements Abstract.Binding {
  private final String myName;

  public Binding(String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }
}
