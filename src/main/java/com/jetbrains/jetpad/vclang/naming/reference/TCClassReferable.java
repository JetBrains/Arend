package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface TCClassReferable extends TCReferable, ClassReferable {
  @Override @Nonnull Collection<? extends TCClassReferable> getSuperClassReferences();
  @Override @Nonnull Collection<? extends TCReferable> getFieldReferables();
  @Override @Nullable TCClassReferable getUnderlyingReference();
}
