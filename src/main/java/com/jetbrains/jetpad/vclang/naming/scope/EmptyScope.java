package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.Collections;
import java.util.Set;

public class EmptyScope implements Scope {
  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public Referable resolveName(String name) {
    return null;
  }
}
