package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

public class StaticNameResolver implements NameResolver {
  private final NameResolver myNameResolver;

  public StaticNameResolver(NameResolver nameResolver) {
    myNameResolver = nameResolver;
  }

  @Override
  public DefinitionPair locateName(String name, boolean isStatic) {
    return myNameResolver.locateName(name, true);
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    return myNameResolver.getMember(parent, name);
  }
}
