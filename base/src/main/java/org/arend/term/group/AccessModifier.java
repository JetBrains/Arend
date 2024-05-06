package org.arend.term.group;

public enum AccessModifier {
  PUBLIC, PROTECTED, PRIVATE;

  public AccessModifier max(AccessModifier modifier) {
    return compareTo(modifier) > 0 ? this : modifier;
  }
}
