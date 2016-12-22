package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;

public class FunctionScope extends OverridingScope {
  public FunctionScope(Scope parent, Namespace staticNamespace) {
    super(parent, staticNamespace);
  }
}
