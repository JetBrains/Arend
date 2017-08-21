package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.Collections;
import java.util.Set;

public class EmptyNamespace implements Namespace {
  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public GlobalReferable resolveName(String name) {
    return null;
  }
}
