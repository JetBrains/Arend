package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class PatternArgument implements Abstract.PatternArgument {
  private final boolean myExplicit;
  private final Pattern myPattern;

  public PatternArgument(Pattern pattern, boolean isExplicit) {
    this.myExplicit = isExplicit;
    this.myPattern = pattern;
  }

  @Override
  public boolean isExplicit() {
    return myExplicit;
  }

  @Override
  public Pattern getPattern() {
    return myPattern;
  }
}
