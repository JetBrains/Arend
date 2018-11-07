package org.arend.typechecking;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.TCReferable;

public interface TypecheckerState {
  Definition record(TCReferable def, Definition res);
  void rewrite(TCReferable def, Definition res);
  Definition getTypechecked(TCReferable def);
  Definition reset(TCReferable def);
  void reset();
}
