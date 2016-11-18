package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.expr.type.Type;

public class TypedBinding extends NamedBinding {
  private Type myType;

  public TypedBinding(String name, Type type) {
    super(name);
    myType = type;
  }

  @Override
  public Type getType() {
    return myType;
  }

  public void setType(Type type) {
    myType = type;
  }

  public String toString() {
    return getName();
  }
}
