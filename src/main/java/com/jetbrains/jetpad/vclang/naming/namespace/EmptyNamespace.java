package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

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
  public Referable resolveInstanceName(String name) {
    return null;
  }
}
