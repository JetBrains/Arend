package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.HashSet;
import java.util.Set;

public class SubScope implements Scope {
  private final Scope parent;
  private final Scope child;

  public SubScope(Scope parent, Scope child) {
    this.parent = parent;
    this.child = child;
  }

  @Override
  public Set<String> getNames() {
    Set<String> names = new HashSet<>(parent.getNames());
    names.addAll(child.getNames());
    return names;
  }

  @Override
  public Referable resolveName(String name) {
    Referable ref = child.resolveName(name);
    return ref != null ? ref : parent.resolveName(name);
  }
}
