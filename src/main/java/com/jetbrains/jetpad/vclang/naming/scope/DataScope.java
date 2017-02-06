package com.jetbrains.jetpad.vclang.naming.scope;

public class DataScope extends OverridingScope {
  public DataScope(Scope parent, Scope staticNamespace) {
    super(parent, staticNamespace);
  }
}
