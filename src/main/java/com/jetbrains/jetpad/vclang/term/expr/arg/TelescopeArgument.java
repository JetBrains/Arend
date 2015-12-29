package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.List;

public class TelescopeArgument extends TypeArgument {
  private final List<String> myNames;

  public TelescopeArgument(boolean explicit, List<String> names, Expression type) {
    super(explicit, type);
    myNames = names;
  }

  @Override
  public TelescopeArgument toExplicit(boolean explicit) {
    return new TelescopeArgument(explicit, myNames, getType());
  }

  public List<String> getNames() {
    return myNames;
  }
}
