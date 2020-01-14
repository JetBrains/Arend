package org.arend.term;

import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface NameRenaming {
  @Nonnull Referable getOldReference();
  @Nullable
  Precedence getPrecedence();
  @Nullable String getName();
}
