package org.arend.naming.reference.converter;

import org.arend.naming.reference.LevelReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;

public class IdReferableConverter implements ReferableConverter {
  public static final IdReferableConverter INSTANCE = new IdReferableConverter();

  private IdReferableConverter() { }

  @Override
  public TCReferable toDataLocatedReferable(LocatedReferable referable) {
    return referable instanceof TCReferable ? (TCReferable) referable : null;
  }

  @Override
  public LevelReferable toDataLevelReferable(Referable referable) {
    return referable instanceof LevelReferable ? (LevelReferable) referable : null;
  }

  @Override
  public Referable convert(Referable referable) {
    return referable;
  }
}
