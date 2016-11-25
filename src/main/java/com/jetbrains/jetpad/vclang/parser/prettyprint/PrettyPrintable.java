package com.jetbrains.jetpad.vclang.parser.prettyprint;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

public interface PrettyPrintable {
  void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent);

  class Util {
    public static String prettyPrint(PrettyPrintable pp) {
      StringBuilder builder = new StringBuilder();
      pp.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 0);
      return builder.toString();
    }
  }
}
