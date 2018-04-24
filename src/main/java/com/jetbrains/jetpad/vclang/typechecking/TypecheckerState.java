package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;

public interface TypecheckerState {
  void record(TCReferable def, Definition res);
  Definition getTypechecked(TCReferable def);
  void reset(TCReferable def);
  void reset();
}
