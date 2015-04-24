package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.List;

public class TelescopeArgument extends TypeArgument implements Abstract.TelescopeArgument {
  private final List<String> myNames;

  public TelescopeArgument(boolean explicit, List<String> names, Expression type) {
    super(explicit, type);
    myNames = names;
  }

  @Override
  public List<String> getNames() {
    return myNames;
  }

  @Override
  public String getName(int index) {
    return myNames.get(index);
  }
}
