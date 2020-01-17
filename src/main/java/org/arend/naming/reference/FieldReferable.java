package org.arend.naming.reference;

public interface FieldReferable extends LocatedReferable {
  boolean isExplicitField();

  boolean isParameterField();

  @Override
  default boolean isClassField() {
    return true;
  }
}
