package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

public class ReferenceConcreteProvider implements ConcreteProvider {
  public static final ConcreteProvider INSTANCE = new ReferenceConcreteProvider();

  private ReferenceConcreteProvider() {}

  @Override
  public Concrete.ReferableDefinition getConcrete(GlobalReferable referable) {
    return referable instanceof GlobalReference ? ((GlobalReference) referable).getDefinition() : null;
  }
}
