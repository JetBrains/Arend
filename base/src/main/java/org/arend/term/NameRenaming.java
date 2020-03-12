package org.arend.term;

import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NameRenaming {
  @NotNull Referable getOldReference();
  @Nullable
  Precedence getPrecedence();
  @Nullable String getName();
}
