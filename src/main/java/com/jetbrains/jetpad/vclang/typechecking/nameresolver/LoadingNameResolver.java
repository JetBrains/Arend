package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

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
        myModuleLoader.load((Namespace) member, true);
      }
      return member;
    }

    member = RootModule.ROOT.getChild(new Utils.Name(name)); // TODO: Do not use getChild.
    ModuleLoadingResult result = myModuleLoader.load((Namespace) member, true);
    if (result == null) {
      RootModule.ROOT.removeChild((Namespace) member);
      return null;
    } else {
      return result.classDefinition != null ? result.classDefinition : member;
    }
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    Definition definition = parent.getDefinition(name);
    if (definition != null) {
      return definition;
    }

    boolean found;
    Namespace child = parent.findChild(name);
    if (child == null) {
      child = parent.getChild(new Utils.Name(name)); // TODO: Do not use getChild.
      found = false;
    } else {
      found = true;
    }
    ModuleLoadingResult result = myModuleLoader.load(child, true);
    if (result == null && !found) {
      parent.removeChild(child);
      return null;
    } else {
      return result != null && result.classDefinition != null ? result.classDefinition : child;
    }
  }
}
