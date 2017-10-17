package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

// TODO[classes]: Get rid of this class
public class NamespaceScope implements Scope {
  private final Namespace myNamespace;

  public NamespaceScope(Namespace namespace) {
    myNamespace = namespace;
  }

  @Nonnull
  @Override
  public Collection<? extends GlobalReferable> getElements() {
    return myNamespace.getElements();
  }

  @Override
  public Referable resolveName(String name) {
    return myNamespace.resolveName(name);
  }
}
