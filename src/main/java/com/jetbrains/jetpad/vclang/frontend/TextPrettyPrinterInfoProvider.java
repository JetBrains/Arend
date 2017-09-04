package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class TextPrettyPrinterInfoProvider implements PrettyPrinterInfoProvider {
  public static final TextPrettyPrinterInfoProvider INSTANCE = new TextPrettyPrinterInfoProvider();

  private TextPrettyPrinterInfoProvider() { }

  @Override
  public String nameFor(Referable referable) {
    return referable.textRepresentation();
  }

  @Override
  public Precedence precedenceOf(GlobalReferable referable) {
    return referable instanceof GlobalReference ? ((GlobalReference) referable).getPrecedence() : Precedence.DEFAULT;
  }
}
