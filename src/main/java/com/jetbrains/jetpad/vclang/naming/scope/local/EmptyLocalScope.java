package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import java.util.Collection;
import java.util.function.Predicate;

public class EmptyLocalScope implements LocalScope {
  private final Scope myGlobalScope;

  public EmptyLocalScope(Scope globalScope) {
    myGlobalScope = globalScope;
  }

  @Override
  public Scope getGlobalScope() {
    return myGlobalScope;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    return myGlobalScope.find(pred);
  }

  @Override
  public Collection<? extends Referable> getElements() {
    return myGlobalScope.getElements();
  }

  @Override
  public Referable resolveName(String name) {
    return myGlobalScope.resolveName(name);
  }

  @Override
  public boolean isEmpty() {
    return myGlobalScope.isEmpty();
  }
}
