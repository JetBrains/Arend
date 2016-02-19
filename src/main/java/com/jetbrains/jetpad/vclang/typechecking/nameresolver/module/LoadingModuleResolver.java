package com.jetbrains.jetpad.vclang.typechecking.nameresolver.module;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;

public class LoadingModuleResolver implements ModuleResolver {
  private final ModuleLoader myModuleLoader;

  public LoadingModuleResolver(ModuleLoader moduleLoader) {
    this.myModuleLoader = moduleLoader;
  }

  @Override
  public NamespaceMember locateModule(ModulePath modulePath) {
    ModuleID moduleID = myModuleLoader.locateModule(modulePath);
    if (moduleID == null)
      return null;
    ModuleLoader.Result result = myModuleLoader.load(moduleID);
    if (result == null)
      return null;
    return result.namespaceMember;
  }
}
