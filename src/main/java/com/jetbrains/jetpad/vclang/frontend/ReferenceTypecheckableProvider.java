package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ModifiableTypecheckableProvider;

public class ReferenceTypecheckableProvider implements ModifiableTypecheckableProvider<Position> {
  public static final ModifiableTypecheckableProvider<Position> INSTANCE = new ReferenceTypecheckableProvider();

  private ReferenceTypecheckableProvider() {}

  @Override
  public Concrete.ReferableDefinition<Position> getTypecheckable(GlobalReferable referable) {
    return ((GlobalReference) referable).getDefinition();
  }

  @Override
  public void setTypecheckable(GlobalReferable referable, Concrete.ReferableDefinition<Position> typecheckable) {
    ((GlobalReference) referable).setDefinition(typecheckable);
  }
}
