package org.arend.naming.reference;

import org.jetbrains.annotations.NotNull;

public interface RedirectingReferable extends GlobalReferable {
  @NotNull Referable getOriginalReferable();

  static @NotNull Referable getOriginalReferable(Referable ref) {
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    return ref;
  }

  @NotNull
  @Override
  default Kind getKind() {
    Referable orig = getOriginalReferable();
    return orig instanceof GlobalReferable ? ((GlobalReferable) orig).getKind() : Kind.OTHER;
  }
}
