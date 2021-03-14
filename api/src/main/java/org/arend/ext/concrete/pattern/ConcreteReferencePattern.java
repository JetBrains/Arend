package org.arend.ext.concrete.pattern;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.Nullable;

public interface ConcreteReferencePattern extends ConcretePattern {
  @Nullable ArendRef getRef();
}
