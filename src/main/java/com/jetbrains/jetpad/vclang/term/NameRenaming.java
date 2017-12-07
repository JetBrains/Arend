package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface NameRenaming {
  @Nonnull Referable getOldReference();
  @Nullable Referable getNewReferable();
  @Nullable Precedence getPrecedence();
}
