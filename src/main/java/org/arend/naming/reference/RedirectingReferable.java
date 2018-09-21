package org.arend.naming.reference;

import javax.annotation.Nonnull;

public interface RedirectingReferable extends GlobalReferable {
  @Nonnull Referable getOriginalReferable();
}
