package org.arend.naming.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DelegateScope implements Scope {
  protected final Scope parent;

  public DelegateScope(Scope parent) {
    this.parent = parent;
  }

  @Override
  public @Nullable Scope resolveNamespace(String name, boolean onlyInternal) {
    return parent.resolveNamespace(name, onlyInternal);
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
