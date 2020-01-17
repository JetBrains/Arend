package org.arend.ext.core.ops;

public enum CMP {
  LE, EQ, GE;

  public CMP not() {
    if (this == LE) return GE;
    if (this == GE) return LE;
    return this;
  }
}
