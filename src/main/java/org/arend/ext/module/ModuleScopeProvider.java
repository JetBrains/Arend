package org.arend.ext.module;

import org.arend.ext.reference.RawScope;

import javax.annotation.Nonnull;

public interface ModuleScopeProvider {
  RawScope forModule(@Nonnull ModulePath module);
}
