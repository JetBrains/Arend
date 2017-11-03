package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Group;

public interface ModuleRegistry {
  void registerModule(ModulePath modulePath, Group group);
  void unregisterModule(ModulePath path);
}
