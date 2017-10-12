package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import java.util.Collection;

// TODO[classes]: Get rid of this class
public class NamespaceScope implements Scope {
  private final Namespace myNamespace;

  public NamespaceScope(Namespace namespace) {
    myNamespace = namespace;
  }

  @Override
  public Collection<? extends GlobalReferable> getElements() {
    return myNamespace.getElements();
  }

  @Override
  public Referable resolveName(String name) {
    return myNamespace.resolveName(name);
  }
}
