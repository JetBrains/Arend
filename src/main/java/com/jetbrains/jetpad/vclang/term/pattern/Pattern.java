package com.jetbrains.jetpad.vclang.term.pattern;


import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.prettyPrintPattern;

public abstract class Pattern implements Abstract.Pattern {
  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintPattern(this, builder, names);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrintPattern(this, builder, new ArrayList<String>());
    return builder.toString();
  }

  public abstract Utils.PatternMatchResult match(Expression expr, List<Binding> context);

  @Override
  public void setWellTyped(Pattern pattern) {

  }
}
