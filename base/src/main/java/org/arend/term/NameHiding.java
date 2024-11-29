package org.arend.term;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;

public interface NameHiding {
  @NotNull Scope.ScopeContext getScopeContext();
  @NotNull Referable getHiddenReference();
}
