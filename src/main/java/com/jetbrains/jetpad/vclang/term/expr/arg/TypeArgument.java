package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class TypeArgument extends Argument implements Abstract.TypeArgument {
  private final Expression myType;

  public TypeArgument(boolean explicit, Expression type) {
    super(explicit);
    myType = type;
  }

  @Override
  public Expression getType() {
    return myType;
  }
}
