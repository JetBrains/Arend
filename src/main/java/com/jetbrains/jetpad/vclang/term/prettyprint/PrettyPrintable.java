package com.jetbrains.jetpad.vclang.term.prettyprint;

import java.util.List;

public interface PrettyPrintable {
  void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent);
}
