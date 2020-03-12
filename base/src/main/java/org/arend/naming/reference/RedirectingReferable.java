package org.arend.naming.reference;

import org.jetbrains.annotations.NotNull;

public interface RedirectingReferable extends GlobalReferable {
  @NotNull Referable getOriginalReferable();
}
