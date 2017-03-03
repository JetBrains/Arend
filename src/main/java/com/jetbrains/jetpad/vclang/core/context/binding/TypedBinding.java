package com.jetbrains.jetpad.vclang.core.context.binding;

import com.jetbrains.jetpad.vclang.core.expr.Expression;

public class TypedBinding extends NamedBinding {
  private Expression myType;

  public TypedBinding(String name, Expression type) {
    super(name);
    myType = type;
  }

  @Override
  public Expression getType() {
    return myType;
  }

  public void setType(Expression type) {
    myType = type;
  }

  public String toString() {
    return getName();
  }
}
