package org.arend.ext.module;

import org.arend.ext.reference.RawScope;
import org.jetbrains.annotations.NotNull;

public interface ModuleScopeProvider {
  RawScope forModule(@NotNull ModulePath module);
}
