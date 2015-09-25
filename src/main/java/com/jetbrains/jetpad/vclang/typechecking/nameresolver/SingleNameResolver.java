package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

public class SingleNameResolver implements NameResolver {
  private final String myName;
  private final DefinitionPair myDefinitionPair;

  public SingleNameResolver(String name, DefinitionPair definitionPair) {
    myName = name;
    myDefinitionPair = definitionPair;
  }

  @Override
  public DefinitionPair locateName(String name, boolean isStatic) {
    return myName.equals(name) ? myDefinitionPair : null;
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    return null;
  }
}
