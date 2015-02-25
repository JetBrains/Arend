package com.jetbrains.jetpad.vclang.term;

import java.io.PrintStream;
import java.util.List;

public interface PrettyPrintable {
  void prettyPrint(PrintStream stream, List<String> names, int prec);
}
