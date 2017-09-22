package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class EmptyScope implements Scope {
  public static final EmptyScope INSTANCE = new EmptyScope();

  private EmptyScope() {}

  @Override
  public Set<Referable> getElements() {
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
