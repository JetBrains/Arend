package org.arend.naming.reference;

import org.jetbrains.annotations.NotNull;

public interface RedirectingReferable extends GlobalReferable {
  @NotNull Referable getOriginalReferable();

  static @NotNull Referable getOriginalReferable(Referable ref) {
    return ref instanceof RedirectingReferable ? ((RedirectingReferable) ref).getOriginalReferable() : ref;
  }
}
