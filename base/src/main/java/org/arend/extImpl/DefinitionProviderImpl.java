package org.arend.extImpl;

import org.arend.core.definition.Definition;
import org.arend.ext.DefinitionProvider;
import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.TCReferable;
import org.jetbrains.annotations.Nullable;

public class DefinitionProviderImpl implements DefinitionProvider {
  public final static DefinitionProvider INSTANCE = new DefinitionProviderImpl();

  private DefinitionProviderImpl() {}

  @Override
  public @Nullable Definition getCoreDefinition(@Nullable ArendRef ref) {
    return ref instanceof TCReferable ? ((TCReferable) ref).getTypechecked() : null;
  }
}
