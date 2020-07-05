package org.arend.naming.reference;

import org.arend.naming.resolving.visitor.TypeClassReferenceExtractVisitor;
import org.jetbrains.annotations.Nullable;

public interface TypedReferable extends Referable {
  default @Nullable ClassReferable getTypeClassReference() {
    return null;
  }

  default @Nullable Referable getBodyReference(TypeClassReferenceExtractVisitor visitor) {
    return null;
  }
}
