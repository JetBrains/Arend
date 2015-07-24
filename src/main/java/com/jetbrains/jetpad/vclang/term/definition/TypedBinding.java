package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class TypedBinding extends Binding {
  private final Expression myType;

  public TypedBinding(String name, Expression type) {
    super(name);
    myType = type;
  }

  @Override
  public Expression getType() {
    return myType;
  }

  @Override
  public Binding lift(int on) {
    return new TypedBinding(getName(), myType.liftIndex(0, on));
  }
}
