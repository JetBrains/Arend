package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface PrettyPrinterInfoProvider extends ParserInfoProvider {
  String nameFor(Referable referable);

  PrettyPrinterInfoProvider TRIVIAL = new PrettyPrinterInfoProvider() {
    @Override
    public String nameFor(Referable referable) {
      return referable.getName();
    }

    @Override
    public Abstract.Precedence precedenceOf(GlobalReferable referable) {
      return Abstract.Precedence.DEFAULT;
    }
  };
}
