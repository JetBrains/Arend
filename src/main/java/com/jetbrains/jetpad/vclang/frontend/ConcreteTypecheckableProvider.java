package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckableProvider;

public class ConcreteTypecheckableProvider implements TypecheckableProvider {
  public static final ConcreteTypecheckableProvider INSTANCE = new ConcreteTypecheckableProvider();

  private ConcreteTypecheckableProvider() { }

  @Override
  public Concrete.Definition forReferable(Abstract.GlobalReferableSourceNode referable) {
    if (referable instanceof Abstract.ClassViewField) {
      referable = ((Abstract.ClassViewField) referable).getUnderlyingField();
    }
    if (referable instanceof Abstract.ClassView) {
      referable = (Concrete.Definition) ((Abstract.ClassView) referable).getUnderlyingClassReference().getReferent();
    }
    return (Concrete.Definition) referable;
  }
}
