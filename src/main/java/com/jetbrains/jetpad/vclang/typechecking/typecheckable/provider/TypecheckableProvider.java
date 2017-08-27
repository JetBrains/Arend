package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface TypecheckableProvider<T> {
  Concrete.ReferableDefinition<T> getTypecheckable(GlobalReferable referable);
}
