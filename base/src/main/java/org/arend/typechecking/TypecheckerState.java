package org.arend.typechecking;

import org.arend.core.definition.Definition;
import org.arend.ext.DefinitionProvider;
import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.TCReferable;
import org.jetbrains.annotations.Nullable;

public interface TypecheckerState extends DefinitionProvider {
  Definition record(TCReferable def, Definition res);
  void rewrite(TCReferable def, Definition res);
  Definition getTypechecked(TCReferable def);
  Definition reset(TCReferable def);
  void reset();

  @Override
  default @Nullable Definition getCoreDefinition(@Nullable ArendRef ref) {
    return ref instanceof TCReferable ? getTypechecked((TCReferable) ref) : null;
  }
}
