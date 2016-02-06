package com.jetbrains.jetpad.vclang.typechecking.nameresolver.module;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;

import java.util.Arrays;
import java.util.List;

public class CompositeModuleResolver implements ModuleResolver {
  private final List<ModuleResolver> myModuleResolvers;

  public CompositeModuleResolver(ModuleResolver... moduleResolvers) {
    this.myModuleResolvers = Arrays.asList(moduleResolvers);
  }

  public void pushNameResolver(ModuleResolver nameResolver) {
    myModuleResolvers.add(nameResolver);
  }

  public void popNameResolver() {
    myModuleResolvers.remove(myModuleResolvers.size() - 1);
  }

  @Override
  public NamespaceMember locateModule(ModulePath modulePath) {
    for (int i = myModuleResolvers.size() - 1; i >= 0; i--) {
      NamespaceMember member = myModuleResolvers.get(i).locateModule(modulePath);
      if (member != null)
        return member;
    }
    return null;
  }
}
