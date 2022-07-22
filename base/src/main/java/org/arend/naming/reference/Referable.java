package org.arend.naming.reference;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

public interface Referable extends ArendRef {
  enum RefKind { EXPR, PLEVEL, HLEVEL }

  @NotNull String textRepresentation();

  @NotNull
  @Override
  default String getRefName() {
    return textRepresentation();
  }

  @NotNull
  default Referable.RefKind getRefKind() {
    return RefKind.EXPR;
  }

  @NotNull
  default Referable getUnderlyingReferable() {
    return this;
  }

  static Referable getUnderlyingReferable(Referable ref) {
    return ref == null ? null : ref.getUnderlyingReferable();
  }

  @Override
  default boolean isLocalRef() {
    return true;
  }
}
