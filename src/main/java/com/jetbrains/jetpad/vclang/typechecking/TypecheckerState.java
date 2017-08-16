package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface TypecheckerState {
  void record(Abstract.GlobalReferableSourceNode def, Definition res);
  Definition getTypechecked(Abstract.GlobalReferableSourceNode def);
  void reset(Abstract.Definition def);
  void reset();
}
