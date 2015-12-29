package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class TypeArgument extends Argument {
  private final Expression myType;

  public TypeArgument(boolean explicit, Expression type) {
    super(explicit);
    myType = type;
  }

  public TypeArgument toExplicit(boolean explicit) {
    return new TypeArgument(explicit, myType);
  }

  public Expression getType() {
    return myType;
  }
}
