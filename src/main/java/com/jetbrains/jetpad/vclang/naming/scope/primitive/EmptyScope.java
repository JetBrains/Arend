package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class EmptyScope implements Scope {
  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public GlobalReferable resolveName(String name) {
    return null;
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    return Collections.emptyList();
  }
}
