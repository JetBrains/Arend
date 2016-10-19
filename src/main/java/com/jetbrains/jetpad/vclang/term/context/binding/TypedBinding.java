package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

public class TypedBinding extends NamedBinding {
  protected Type myType;

  public TypedBinding(String name, Type type) {
    super(name);
    myType = type;
  }

  @Override
  public Type getType() {
    return myType;
  }

  public String toString() {
    return getName();
  }
}
