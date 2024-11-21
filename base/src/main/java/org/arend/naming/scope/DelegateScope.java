package org.arend.naming.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DelegateScope implements Scope {
  protected final Scope parent;

  public DelegateScope(Scope parent) {
    this.parent = parent;
  }

  @Override
  public @Nullable Scope resolveNamespace(@NotNull String name) {
    return parent.resolveNamespace(name);
  }

  @Override
  public @NotNull Scope getGlobalSubscope() {
    return parent.getGlobalSubscope();
  }

  @Override
  public @NotNull Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    return parent.getGlobalSubscopeWithoutOpens(withImports);
  }

  @Override
  public @Nullable ImportedScope getImportedSubscope() {
    return parent.getImportedSubscope();
  }
}
