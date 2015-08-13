package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.List;

public class ConstructorPattern extends Pattern implements Abstract.ConstructorPattern {
  private final Utils.Name myConstructorName;
  private final List<Pattern> myArguments;

  public ConstructorPattern(Utils.Name name, List<Pattern> arguments) {
    myConstructorName = name;
    myArguments = arguments;
  }

  @Override
  public Utils.Name getConstructorName() {
    return myConstructorName;
  }

  @Override
  public List<Pattern> getArguments() {
    return myArguments;
  }
}
