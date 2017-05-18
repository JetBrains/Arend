package com.jetbrains.jetpad.vclang.core.elimtree;

public class EmptyPattern implements Pattern {
  public final static EmptyPattern INSTANCE = new EmptyPattern();
  private EmptyPattern() {}
}
