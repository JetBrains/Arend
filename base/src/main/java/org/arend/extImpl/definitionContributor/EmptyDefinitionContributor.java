package org.arend.extImpl.definitionContributor;

import org.arend.ext.DefinitionContributor;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.NotNull;

public class EmptyDefinitionContributor implements DefinitionContributor {
  public static final EmptyDefinitionContributor INSTANCE = new EmptyDefinitionContributor();

  private EmptyDefinitionContributor() {}

  @Override
  public void declare(@NotNull ModulePath module, @NotNull LongName name, @NotNull Precedence precedence, @NotNull MetaDefinition meta) {

  }
}
