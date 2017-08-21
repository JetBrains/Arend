package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface ModuleRegistry {
  ModuleNamespace registerModule(ModulePath modulePath, Concrete.ClassDefinition module);
  void unregisterModule(ModulePath path);
}
