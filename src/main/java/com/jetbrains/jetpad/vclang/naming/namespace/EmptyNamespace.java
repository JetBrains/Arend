package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class EmptyNamespace implements Namespace {
  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    return null;
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    return Collections.emptyList();
  }
}
