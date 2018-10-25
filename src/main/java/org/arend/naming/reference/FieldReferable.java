package org.arend.naming.reference;

public interface FieldReferable extends LocatedReferable {
  boolean isExplicitField();

  boolean isParameterField();
}
