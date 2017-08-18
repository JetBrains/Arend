package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface PrettyPrinterInfoProvider extends ParserInfoProvider {
  String nameFor(Abstract.ReferableSourceNode referable);

  PrettyPrinterInfoProvider TRIVIAL = new PrettyPrinterInfoProvider() {
    @Override
    public String nameFor(Abstract.ReferableSourceNode referable) {
      return referable.getName();
    }

    @Override
    public Abstract.Precedence precedenceOf(Abstract.GlobalReferableSourceNode referable) {
      return Abstract.Precedence.DEFAULT;
    }
  };
}
