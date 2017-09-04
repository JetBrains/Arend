package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.TypecheckableProvider;

public class ReferenceTypecheckableProvider implements TypecheckableProvider<Position> {
  public static final TypecheckableProvider<Position> INSTANCE = new ReferenceTypecheckableProvider();

  private ReferenceTypecheckableProvider() {}

  @Override
  public Concrete.ReferableDefinition<Position> getTypecheckable(GlobalReferable referable) {
    return referable instanceof GlobalReference ? ((GlobalReference) referable).getDefinition() : null;
  }
}
