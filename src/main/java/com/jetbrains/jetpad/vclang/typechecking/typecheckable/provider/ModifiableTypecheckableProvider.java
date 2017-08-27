package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface ModifiableTypecheckableProvider<T> extends TypecheckableProvider<T> {
  void setTypecheckable(GlobalReferable referable, Concrete.ReferableDefinition<T> typecheckable);
}
