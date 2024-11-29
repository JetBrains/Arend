package org.arend.term;

import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.NamedUnresolvedReference;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NameRenaming {
  @NotNull Scope.ScopeContext getScopeContext();
  @NotNull NamedUnresolvedReference getOldReference();
  @Nullable Precedence getPrecedence();
  @Nullable String getName();
}
