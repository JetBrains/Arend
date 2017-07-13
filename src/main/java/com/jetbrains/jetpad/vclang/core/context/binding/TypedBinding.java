package com.jetbrains.jetpad.vclang.core.context.binding;

import com.jetbrains.jetpad.vclang.core.expr.Expression;

public class TypedBinding extends NamedBinding {
  private final Expression myType;

  public TypedBinding(String name, Expression type) {
    super(name);
    myType = type;
  }

  @Override
  public Expression getTypeExpr() {
    return myType;
  }

  public String toString() {
    return getName();
  }
}
