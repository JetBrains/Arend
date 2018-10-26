package org.arend.typechecking;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.TCReferable;

public interface TypecheckerState {
  void record(TCReferable def, Definition res);
  void rewrite(TCReferable def, Definition res);
  Definition getTypechecked(TCReferable def);
  void reset(TCReferable def);
  void reset();
}
