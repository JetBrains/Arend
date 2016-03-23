package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class TypedBinding extends NamedBinding {
  private final Expression myType;

  public TypedBinding(String name, Expression type) {
    super(name);
    myType = type;
  }

  @Override
  public Expression getType() {
    return myType;
  }

  public String toString() {
    return getName() + " : " + getType();
  }
}
