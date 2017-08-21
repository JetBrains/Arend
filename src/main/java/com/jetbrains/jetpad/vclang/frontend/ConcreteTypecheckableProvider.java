package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.text.Position;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckableProvider;

public class ConcreteTypecheckableProvider implements TypecheckableProvider<Position> {
  public static final ConcreteTypecheckableProvider INSTANCE = new ConcreteTypecheckableProvider();

  private ConcreteTypecheckableProvider() { }

  @Override
  public Concrete.Definition<Position> forReferable(GlobalReferable referable) {
    if (referable instanceof Abstract.ClassViewField) {
      referable = ((Abstract.ClassViewField) referable).getUnderlyingField();
    }
    if (referable instanceof Abstract.ClassView) {
      referable = (Concrete.Definition) ((Abstract.ClassView) referable).getUnderlyingClassReference().getReferent();
    }
    return (Concrete.Definition<Position>) referable; // TODO[abstract]
  }
}
