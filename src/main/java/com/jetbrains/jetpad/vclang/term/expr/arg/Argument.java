package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintArgument;

public abstract class Argument implements Abstract.Argument {
  private final boolean myExplicit;

  public Argument(boolean explicit) {
    myExplicit = explicit;
  }

  @Override
  public boolean getExplicit() {
    return myExplicit;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintArgument(this, builder, names, prec, 0);
  }
}
