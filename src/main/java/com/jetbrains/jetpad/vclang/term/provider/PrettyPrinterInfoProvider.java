package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

public interface PrettyPrinterInfoProvider {
  String nameFor(Referable referable);

  PrettyPrinterInfoProvider TRIVIAL = Referable::textRepresentation;
}
