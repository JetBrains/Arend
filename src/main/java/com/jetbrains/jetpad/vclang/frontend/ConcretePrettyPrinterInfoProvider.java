package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class ConcretePrettyPrinterInfoProvider implements PrettyPrinterInfoProvider {
  public static final ConcretePrettyPrinterInfoProvider INSTANCE = new ConcretePrettyPrinterInfoProvider();

  private ConcretePrettyPrinterInfoProvider() { }

  @Override
  public String nameFor(Abstract.ReferableSourceNode referable) {
    return referable.getName();
  }

  @Override
  public Abstract.Precedence precedenceOf(Abstract.GlobalReferableSourceNode referable) {
    return referable instanceof Concrete.Definition ? ((Concrete.Definition) referable).getPrecedence() : Abstract.Precedence.DEFAULT; // TODO[references]
  }
}
