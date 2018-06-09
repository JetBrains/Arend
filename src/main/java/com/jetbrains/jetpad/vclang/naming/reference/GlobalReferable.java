package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public interface GlobalReferable extends TypedReferable {
  @Nonnull Precedence getPrecedence();

  default GlobalReferable getTypecheckable() {
    return this;
  }

  default boolean isTypecheckable() {
    return true;
  }
}
