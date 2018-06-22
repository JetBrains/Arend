package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nullable;

public interface TCReferable extends LocatedReferable {
  TCReferable getTypecheckable();
  @Nullable TCReferable getUnderlyingReference();
}
