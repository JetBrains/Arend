package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface TypecheckerState {
  void record(GlobalReferable def, Definition res);
  Definition getTypechecked(GlobalReferable def);
  void reset(Abstract.Definition def);
  void reset();
}
