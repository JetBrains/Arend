package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class PatternArgument implements Abstract.PatternArgument {
  private final boolean myExplicit;
  private final boolean myHidden;
  private final Pattern myPattern;

  public PatternArgument(Pattern pattern, boolean isExplicit, boolean isHidden) {
    this.myExplicit = isExplicit;
    this.myHidden = isHidden;
    this.myPattern = pattern;
  }

  @Override
  public boolean isHidden() {
    return myHidden;
  }

  @Override
  public boolean isExplicit() {
    return myExplicit;
  }

  @Override
  public Pattern getPattern() {
    return myPattern;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    Utils.prettyPrintPatternArg(this, builder, names);
  }
}
