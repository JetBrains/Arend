package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nullable;

public interface ConcreteProvider {
  @Nullable Concrete.ReferableDefinition getConcrete(GlobalReferable referable);
  @Nullable Concrete.FunctionDefinition getConcreteFunction(GlobalReferable referable);
  @Nullable Concrete.Instance getConcreteInstance(GlobalReferable referable);
  @Nullable Concrete.ClassDefinition getConcreteClass(ClassReferable referable);
}
