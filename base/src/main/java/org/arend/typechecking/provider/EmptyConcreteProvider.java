package org.arend.typechecking.provider;

import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.Nullable;

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
  public Concrete.FunctionDefinition getConcreteInstance(GlobalReferable referable) {
    return null;
  }

  @Nullable
  @Override
  public Concrete.ClassDefinition getConcreteClass(GlobalReferable referable) {
    return null;
  }

  @Nullable
  @Override
  public Concrete.DataDefinition getConcreteData(GlobalReferable referable) {
    return null;
  }
}
