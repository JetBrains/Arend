package com.jetbrains.jetpad.vclang.naming.reference.converter;

import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;

public class IdReferableConverter implements ReferableConverter {
  public static final IdReferableConverter INSTANCE = new IdReferableConverter();

  private IdReferableConverter() { }

  @Override
  public Referable toDataReferable(Referable referable) {
    return referable;
  }

  @Override
  public TCReferable toDataLocatedReferable(LocatedReferable referable) {
    return referable instanceof TCReferable ? (TCReferable) referable : null;
  }
}
