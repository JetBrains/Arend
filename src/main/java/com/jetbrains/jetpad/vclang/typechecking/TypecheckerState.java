package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface TypecheckerState {
  void record(Abstract.Definition def, Definition res);
  Definition getTypechecked(Abstract.Definition def);
  void reset(Abstract.Definition def);
  void reset();
}
