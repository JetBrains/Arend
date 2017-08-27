package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class NamespaceScope implements Scope {
  private final Namespace myNamespace;

  public NamespaceScope(Namespace namespace) {
    myNamespace = namespace;
  }

  @Override
  public Set<String> getNames() {
    return myNamespace.getNames();
  }

  @Override
  public Referable resolveName(String name) {
    return myNamespace.resolveName(name);
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    return Collections.emptyList();
  }
}
