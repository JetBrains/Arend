package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.*;

public class OverridingScope implements Scope {
  private final Scope myParent;
  private final Scope myChild;

  public OverridingScope(Scope parent, Scope child) {
    myParent = parent;
    myChild = child;
  }

  @Override
  public Set<String> getNames() {
    Set<String> names = new HashSet<>(myParent.getNames());
    names.addAll(myChild.getNames());
    return names;
  }

  @Override
  public Referable resolveName(String name) {
    Referable ref = myChild.resolveName(name);
    return ref != null ? ref : myParent.resolveName(name);
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    List<Concrete.Instance> instances = new ArrayList<>(myParent.getInstances());
    instances.addAll(myChild.getInstances());
    return instances;
  }
}
