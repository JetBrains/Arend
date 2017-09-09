package com.jetbrains.jetpad.vclang.term;

import javax.annotation.Nonnull;

public class Precedence {
  public enum Associativity { LEFT_ASSOC, RIGHT_ASSOC, NON_ASSOC }

  public static final Precedence DEFAULT = new Precedence(Associativity.RIGHT_ASSOC, (byte) 10);

  public final @Nonnull
  Associativity associativity;
  public final byte priority;

  public Precedence(@Nonnull Associativity associativity, byte priority) {
    this.associativity = associativity;
    this.priority = priority;
  }

  public Precedence(byte prec) {
    this.associativity = Associativity.NON_ASSOC;
    this.priority = prec;
  }

  @Override
  public String toString() {
    String result = "infix";
    if (associativity == Associativity.LEFT_ASSOC) {
      result += "l";
    }
    if (associativity == Associativity.RIGHT_ASSOC) {
      result += "r";
    }
    return result + " " + priority;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Precedence that = (Precedence) o;
    return priority == that.priority && associativity == that.associativity;
  }

  @Override
  public int hashCode() {
    return  31 * associativity.hashCode() + (int) priority;
  }
}
