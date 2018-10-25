package org.arend.naming.reference;

public interface FieldReferable extends LocatedReferable {
  boolean isExplicitField();

  default boolean isParameterField() {
    return !isExplicitField();
  }
}
