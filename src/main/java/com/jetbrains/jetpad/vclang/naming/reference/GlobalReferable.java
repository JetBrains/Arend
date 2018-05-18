package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface GlobalReferable extends Referable {
  @Nonnull Precedence getPrecedence();

  default GlobalReferable getTypecheckable() {
    return this;
  }

  default boolean isTypecheckable() {
    return true;
  }

  default @Nullable ClassReferable getTypeClassReference() {
    return null;
  }
}
