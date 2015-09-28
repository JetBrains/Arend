package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.*;

public class LoadingNameResolver implements NameResolver {
  private final ModuleLoader myModuleLoader;
  private final NameResolver myNameResolver;

  public LoadingNameResolver(ModuleLoader moduleLoader, NameResolver nameResolver) {
    myModuleLoader = moduleLoader;
    myNameResolver = nameResolver;
  }

  @Override
  public DefinitionPair locateName(String name, boolean isStatic) {
    DefinitionPair member = myNameResolver.locateName(name, isStatic);
    if (member != null) {
      if (member.definition == null && member.abstractDefinition == null) {
        ModuleLoadingResult result = myModuleLoader.load(member.namespace.getParent(), member.namespace.getName().name, true);
        if (result != null) {
          return result.definition;
        }
      }
      return member;
    }

    ModuleLoadingResult result = myModuleLoader.load(RootModule.ROOT, name, true);
    return result == null ? null : result.definition;
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    DefinitionPair member = parent.getMember(name);
    if (member == null) {
      return null;
    }

    if (member.definition == null && member.abstractDefinition == null) {
      ModuleLoadingResult result = myModuleLoader.load(parent, name, true);
      if (result != null && result.definition != null) {
        member.abstractDefinition = result.definition.abstractDefinition;
        member.definition = result.definition.definition;
      }
    }

    return member;
  }
}
