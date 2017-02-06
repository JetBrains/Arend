package com.jetbrains.jetpad.vclang.naming.scope;

public class FunctionScope extends OverridingScope {
  public FunctionScope(Scope parent, Scope staticNamespace) {
    super(parent, staticNamespace);
  }
}
