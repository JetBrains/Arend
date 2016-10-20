package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;

public class DataScope extends OverridingScope {
  public DataScope(Scope parent, Namespace staticNamespace) {
    super(parent, staticNamespace);
  }
}
