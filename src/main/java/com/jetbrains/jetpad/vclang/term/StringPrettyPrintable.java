package com.jetbrains.jetpad.vclang.term;

import java.util.List;

public class StringPrettyPrintable implements PrettyPrintable {
  private final String myString;

  public StringPrettyPrintable(String string) {
    myString = string;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    builder.append(myString);
  }
}
