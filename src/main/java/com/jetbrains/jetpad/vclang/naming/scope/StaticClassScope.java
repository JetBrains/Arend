package com.jetbrains.jetpad.vclang.naming.scope;

public class StaticClassScope extends OverridingScope {
  public StaticClassScope(Scope parent, Scope staticNamespace) {
    super(parent, staticNamespace);
  }
}
