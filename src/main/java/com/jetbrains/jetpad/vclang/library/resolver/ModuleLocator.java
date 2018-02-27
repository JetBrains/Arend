package com.jetbrains.jetpad.vclang.library.resolver;

import com.jetbrains.jetpad.vclang.library.Library;
import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nullable;

public interface ModuleLocator {
  /**
   * Finds the library containing a specified module.
   *
   * @param modulePath    the path to a module.
   *
   * @return the library containing the module or null if the module does not belong to any library.
   */
  @Nullable
  Library locate(ModulePath modulePath);
}
