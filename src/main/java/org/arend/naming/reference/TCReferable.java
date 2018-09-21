package org.arend.naming.reference;

import javax.annotation.Nullable;

public interface TCReferable extends LocatedReferable {
  TCReferable getTypecheckable();
  @Nullable TCReferable getUnderlyingReference();
}
