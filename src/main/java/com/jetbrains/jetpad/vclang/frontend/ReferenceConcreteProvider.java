package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

public class ReferenceConcreteProvider implements ConcreteProvider {
  public static final ConcreteProvider INSTANCE = new ReferenceConcreteProvider();

  private ReferenceConcreteProvider() {}

  @Override
  public Concrete.ReferableDefinition getConcrete(GlobalReferable referable) {
    return referable instanceof ConcreteGlobalReferable ? ((ConcreteGlobalReferable) referable).getDefinition() : null;
  }
}
