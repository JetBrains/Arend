package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;

public class DynamicClassScope extends SubScope {
  public DynamicClassScope(Scope parent, Namespace staticNamespace, Namespace dynamicNamespace) {
    super(parent, new MergeScope(staticNamespace, dynamicNamespace));
  }
}
