package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;

public class DynamicClassScope extends OverridingScope {
  public DynamicClassScope(Scope parent, Namespace staticNamespace, Namespace dynamicNamespace) {
    super(parent, new MergeScope(staticNamespace, dynamicNamespace));
  }

  public DynamicClassScope(Scope parent, Scope child) {
    super(parent, child);
  }
}
