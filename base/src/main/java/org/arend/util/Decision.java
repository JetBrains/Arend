package org.arend.util;

public enum Decision {
  NO, MAYBE, YES;

  public Decision max(Decision other) {
    return ordinal() >= other.ordinal() ? this : other;
  }

  public Decision min(Decision other) {
    return ordinal() <= other.ordinal() ? this : other;
  }
}
