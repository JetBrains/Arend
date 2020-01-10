package org.arend.ext.module;

import org.arend.ext.reference.ArendScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ModuleScopeProvider {
  @Nullable
  ArendScope forModule(@Nonnull ModulePath module);
}
