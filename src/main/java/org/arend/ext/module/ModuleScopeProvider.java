package org.arend.ext.module;

import org.arend.ext.reference.RawScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ModuleScopeProvider {
  @Nullable
  RawScope forModule(@Nonnull ModulePath module);
}
