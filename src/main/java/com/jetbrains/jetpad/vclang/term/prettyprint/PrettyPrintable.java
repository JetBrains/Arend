package com.jetbrains.jetpad.vclang.term.prettyprint;

import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public interface PrettyPrintable {
  String prettyPrint(PrettyPrinterInfoProvider infoProvider);
}
