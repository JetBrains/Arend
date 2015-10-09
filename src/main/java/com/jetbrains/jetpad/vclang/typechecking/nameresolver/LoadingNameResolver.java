package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public class LoadingNameResolver implements NameResolver {
  private final ModuleLoader myModuleLoader;
  private final NameResolver myNameResolver;

  public LoadingNameResolver(ModuleLoader moduleLoader, NameResolver nameResolver) {
    myModuleLoader = moduleLoader;
    myNameResolver = nameResolver;
  }

  @Override
  public NamespaceMember locateName(String name) {
    NamespaceMember member = myNameResolver.locateName(name);
    ModuleLoadingResult result = myModuleLoader.load(member != null ? member.getResolvedName() : new ResolvedName(RootModule.ROOT, name), true);
    return result != null && result.namespaceMember != null ? result.namespaceMember : member;
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    NamespaceMember member = parent.getMember(name);
    if (member == null) {
      return null;
    }

    ModuleLoadingResult result = myModuleLoader.load(member.getResolvedName(), true);
    return result != null && result.namespaceMember != null ? result.namespaceMember : member;
  }
}
