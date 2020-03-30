package org.arend.ext;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.NotNull;

public interface DefinitionContributor {
  void declare(@NotNull ModulePath module, @NotNull LongName name, @NotNull String description, @NotNull Precedence precedence, @NotNull MetaDefinition meta);
}
