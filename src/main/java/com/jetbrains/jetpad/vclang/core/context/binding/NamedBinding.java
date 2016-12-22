package com.jetbrains.jetpad.vclang.core.context.binding;

public abstract class NamedBinding implements Binding {
  private final String myName;

  public NamedBinding(String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }
}
