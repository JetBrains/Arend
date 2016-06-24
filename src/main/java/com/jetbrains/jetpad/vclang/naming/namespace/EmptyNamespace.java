package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.Set;

public class EmptyNamespace implements Namespace {
  public static final EmptyNamespace INSTANCE = new EmptyNamespace();

  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    return null;
  }
}
