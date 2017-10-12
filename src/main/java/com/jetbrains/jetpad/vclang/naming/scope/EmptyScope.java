package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

public class EmptyScope implements Scope {
  public final static EmptyScope INSTANCE = new EmptyScope();

  private EmptyScope() {}

  @Override
  public Referable find(Predicate<Referable> pred) {
    return null;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public Collection<? extends Referable> getElements() {
    return Collections.emptyList();
  }

  @Override
  public Referable resolveName(String name) {
    return null;
  }
}
