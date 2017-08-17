package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.jetbrains.jetpad.vclang.term.Abstract;

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
  public Abstract.ReferableSourceNode resolveName(String name) {
    Abstract.ReferableSourceNode ref = myChild.resolveName(name);
    return ref != null ? ref : myParent.resolveName(name);
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    List<Abstract.ClassViewInstance> instances = new ArrayList<>(myParent.getInstances());
    instances.addAll(myChild.getInstances());
    return instances;
  }
}
