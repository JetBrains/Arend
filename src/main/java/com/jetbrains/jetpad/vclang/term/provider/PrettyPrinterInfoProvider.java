package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface PrettyPrinterInfoProvider extends ParserInfoProvider {
  String nameFor(Abstract.ReferableSourceNode referable);
}
