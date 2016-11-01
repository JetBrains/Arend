package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.GlobalInstancePool;

public interface TypecheckerState {
  void record(Abstract.Definition def, Definition res);

  Definition getTypechecked(Abstract.Definition def);

  GlobalInstancePool getInstancePool();
}
