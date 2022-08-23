package org.arend.naming.reference;

public interface FieldReferable extends LocatedReferable {
  boolean isExplicitField();

  boolean isParameterField();

  default boolean isRealParameterField() {
    return false;
  }

  @Override
  default boolean isClassField() {
    return true;
  }
}
