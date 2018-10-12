package org.arend.naming.reference;

import javax.annotation.Nullable;

public interface TCReferable extends LocatedReferable {
  TCReferable getTypecheckable();
  @Nullable TCReferable getUnderlyingReference();

  default @Override @Nullable TCReferable getUnderlyingTypecheckable() {
    TCReferable underlyingRef = getUnderlyingReference();
    return underlyingRef == null ? this : underlyingRef.isSynonym() ? null : underlyingRef;
  }

  @Override
  default boolean isSynonym() {
    return getUnderlyingReference() != null;
  }
}
