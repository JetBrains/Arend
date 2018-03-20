package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface ClassReferable extends LocatedReferable {
  @Nonnull Collection<? extends ClassReferable> getSuperClassReferences();
  @Nonnull Collection<? extends GlobalReferable> getFieldReferables();
}
