package org.arend.module;

import org.arend.term.group.Group;

public interface ModuleRegistry {
  void registerModule(ModulePath modulePath, Group group);
  void unregisterModule(ModulePath path);
  boolean isRegistered(ModulePath modulePath);
}
