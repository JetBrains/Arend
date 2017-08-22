package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Concrete;

public interface ModuleNamespaceProvider {
  ModuleNamespace forModule(Concrete.ClassDefinition definition);
  ModuleNamespace root();
}
