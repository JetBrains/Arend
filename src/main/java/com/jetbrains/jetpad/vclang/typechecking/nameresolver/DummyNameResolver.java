package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

public class DummyNameResolver implements NameResolver {
  private DummyNameResolver() {
  }

  private final static DummyNameResolver INSTANCE = new DummyNameResolver();

  public static DummyNameResolver getInstance() {
    return INSTANCE;
  }

  @Override
  public DefinitionPair locateName(String name, boolean isStatic) {
    return null;
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    return parent.getMember(name);
  }
}
