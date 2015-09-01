package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

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
    if (member != null) {
      if (member instanceof Namespace) {
        myModuleLoader.load(((Namespace) member).getParent(), member.getName().name, true);
      }
      return member;
    }

    ModuleLoadingResult result = myModuleLoader.load(RootModule.ROOT, name, true);
    if (result == null) {
      return null;
    } else {
      return result.classDefinition != null ? result.classDefinition : result.namespace;
    }
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    Definition definition = parent.getDefinition(name);
    if (definition != null) {
      return definition;
    }

    Namespace child = parent.findChild(name);
    if (child == null) {
      return null;
    }
    ModuleLoadingResult result = myModuleLoader.load(parent, name, true);
    return result != null && result.classDefinition != null ? result.classDefinition : child;
  }
}
