package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ModuleRegistry {
  ModuleNamespace registerModule(ModulePath modulePath, Abstract.ClassDefinition module);
  void unregisterModule(ModulePath path);
}
