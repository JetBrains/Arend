package com.jetbrains.jetpad.vclang.term.pattern;


import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintPattern;

public abstract class Pattern implements Abstract.Pattern {
  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintPattern(this, builder, names, 0);
  }
}
