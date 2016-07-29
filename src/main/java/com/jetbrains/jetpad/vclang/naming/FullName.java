package com.jetbrains.jetpad.vclang.naming;

public class FullName {
  private final FullName myParent;
  private final String myName;

  public FullName(String name) {
    this(null, name);
  }

  public FullName(FullName parent, String name) {
    myParent = parent;
    myName = name;
  }

  @Override
  public String toString() {
    return (myParent != null ? myParent.toString() + "." : "") + myName;
  }
}
