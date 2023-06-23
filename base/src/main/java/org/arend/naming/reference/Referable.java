package org.arend.naming.reference;

import org.arend.ext.core.context.CoreInferenceVariable;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Referable extends ArendRef {
  @NotNull String textRepresentation();

  @NotNull
  @Override
  default String getRefName() {
    return textRepresentation();
  }

  @NotNull
  @Override
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

  @Override
  default boolean isInferenceRef() {
    return false;
  }

  @Override
  default @Nullable CoreInferenceVariable getInferenceVariable() {
    return null;
  }
}
