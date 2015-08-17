package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.List;

public class ConstructorPattern extends Pattern implements Abstract.ConstructorPattern {
  private final Constructor myConstructor;
  private final List<Pattern> myArguments;

  public ConstructorPattern(Constructor constructor, List<Pattern> arguments, boolean isExplicit) {
    super(isExplicit);
    myConstructor = constructor;
    myArguments = arguments;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }
  @Override
  public Utils.Name getConstructorName() {
    return myConstructor.getName();
  }

  @Override
  public List<Pattern> getArguments() {
    return myArguments;
  }
}
