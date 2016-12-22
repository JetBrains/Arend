package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;

public class DynamicClassScope extends OverridingScope {
  public DynamicClassScope(Scope parent, Namespace staticNamespace, Namespace dynamicNamespace) {
    super(parent, new MergeScope(staticNamespace, dynamicNamespace));
  }

  public DynamicClassScope(Scope parent, Namespace staticNamespace, Namespace dynamicNamespace, ErrorReporter errorReporter) {
    super(parent, OverridingScope.merge(staticNamespace, dynamicNamespace, errorReporter));
  }
}
