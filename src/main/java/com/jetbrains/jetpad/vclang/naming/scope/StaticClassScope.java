package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;

public class StaticClassScope extends SubScope {
  public StaticClassScope(Scope parent, Namespace staticNamespace) {
    super(parent, staticNamespace);
  }
}
