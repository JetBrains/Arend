package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface TypecheckableProvider {
  Concrete.ReferableDefinition getTypecheckable(GlobalReferable referable);
}
