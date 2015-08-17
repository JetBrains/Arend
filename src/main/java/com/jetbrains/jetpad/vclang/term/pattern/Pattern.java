package com.jetbrains.jetpad.vclang.term.pattern;


import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.prettyPrintPattern;

public abstract class Pattern implements Abstract.Pattern {
  private final boolean myExplicit;

  public Pattern(boolean isExplicit) {
    myExplicit = isExplicit;
  }

  public boolean getExplicit() {
    return myExplicit;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintPattern(this, builder, names);
  }
}
