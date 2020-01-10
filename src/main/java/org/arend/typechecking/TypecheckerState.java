package org.arend.typechecking;

import org.arend.core.definition.Definition;
import org.arend.ext.DefinitionProvider;
import org.arend.ext.concrete.ArendRef;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.naming.reference.TCReferable;

import javax.annotation.Nullable;

public interface TypecheckerState extends DefinitionProvider {
  Definition record(TCReferable def, Definition res);
  void rewrite(TCReferable def, Definition res);
  Definition getTypechecked(TCReferable def);
  Definition reset(TCReferable def);
  void reset();

  @Nullable
  @Override
  default CoreDefinition getDefinition(ArendRef ref) {
    return ref instanceof TCReferable ? getTypechecked((TCReferable) ref) : null;
  }
}
