package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class NameArgument extends Argument implements Abstract.NameArgument {
  private final String myName;

  public NameArgument(boolean explicit, String name) {
    super(explicit);
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }
}
