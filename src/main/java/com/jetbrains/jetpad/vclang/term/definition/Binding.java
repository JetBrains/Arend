package com.jetbrains.jetpad.vclang.term.definition;

public class Binding {
  private final String myName;
  private final Signature mySignature;

  public Binding(String name, Signature signature) {
    myName = name;
    mySignature = signature;
  }

  public String getName() {
    return myName;
  }

  public Signature getSignature() {
    return mySignature;
  }
}
