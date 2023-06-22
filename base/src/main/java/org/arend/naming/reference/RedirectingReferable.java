package org.arend.naming.reference;

import org.arend.term.group.AccessModifier;
import org.jetbrains.annotations.NotNull;

public interface RedirectingReferable extends GlobalReferable {
  @NotNull Referable getOriginalReferable();

  static Referable getOriginalReferable(Referable ref) {
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    return ref;
  }

  @Override
  default @NotNull AccessModifier getAccessModifier() {
    Referable ref = getOriginalReferable();
    return ref instanceof GlobalReferable ? ((GlobalReferable) ref).getAccessModifier() : AccessModifier.PUBLIC;
  }

  @NotNull
  @Override
  default Kind getKind() {
    Referable orig = getOriginalReferable();
    return orig instanceof GlobalReferable ? ((GlobalReferable) orig).getKind() : Kind.OTHER;
  }

  @Override
  @NotNull
  default Referable.RefKind getRefKind() {
    return getOriginalReferable().getRefKind();
  }
}
