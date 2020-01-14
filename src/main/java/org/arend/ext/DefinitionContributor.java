package org.arend.ext;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;

import javax.annotation.Nonnull;

public interface DefinitionContributor {
  void declare(@Nonnull ModulePath module, @Nonnull LongName name, @Nonnull Precedence precedence, @Nonnull MetaDefinition meta);
}
