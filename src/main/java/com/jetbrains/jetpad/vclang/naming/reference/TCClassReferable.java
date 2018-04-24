package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface TCClassReferable extends TCReferable, ClassReferable {
  @Nonnull Collection<? extends TCClassReferable> getSuperClassReferences();
  @Nonnull Collection<? extends TCReferable> getFieldReferables();
}
