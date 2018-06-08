package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nullable;

public class EmptyConcreteProvider implements ConcreteProvider {
  public final static EmptyConcreteProvider INSTANCE = new EmptyConcreteProvider();

  private EmptyConcreteProvider() {}

  @Nullable
  @Override
  public Concrete.ReferableDefinition getConcrete(GlobalReferable referable) {
    return null;
  }

  @Nullable
  @Override
  public Concrete.FunctionDefinition getConcreteFunction(GlobalReferable referable) {
    return null;
  }

  @Nullable
  @Override
  public Concrete.Instance getConcreteInstance(GlobalReferable referable) {
    return null;
  }

  @Nullable
  @Override
  public Concrete.ClassDefinition getConcreteClass(ClassReferable referable) {
    return null;
  }
}
