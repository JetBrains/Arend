package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.term.Group;

public interface ModuleRegistry {
  ModuleNamespace registerModule(ModulePath modulePath, Group group);
  void unregisterModule(ModulePath path);
}
