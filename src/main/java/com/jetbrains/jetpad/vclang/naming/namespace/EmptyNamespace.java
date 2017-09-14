package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EmptyNamespace implements Namespace {
  public static final EmptyNamespace INSTANCE = new EmptyNamespace();

  private EmptyNamespace() {}

  @Override
  public List<GlobalReferable> getElements() {
    return Collections.emptyList();
  }

  @Override
  public GlobalReferable resolveName(String name) {
    return null;
  }
}
