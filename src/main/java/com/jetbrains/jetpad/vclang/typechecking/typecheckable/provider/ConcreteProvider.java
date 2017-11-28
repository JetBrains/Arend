package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nullable;

public interface ConcreteProvider {
  @Nullable Concrete.ReferableDefinition getConcrete(GlobalReferable referable);
}
