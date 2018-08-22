package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

public class FieldReferableImpl extends DataLocatedReferableImpl implements TCFieldReferable {
  public FieldReferableImpl(Precedence precedence, String name, LocatedReferable parent, TCClassReferable typeClassReferable) {
    super(precedence, name, parent, typeClassReferable, Kind.FIELD);
  }

  @Override
  public boolean isExplicitField() {
    return true;
  }
}
