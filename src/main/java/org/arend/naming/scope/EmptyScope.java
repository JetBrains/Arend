package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;

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

  @NotNull
  @Override
  public Collection<? extends Referable> getElements() {
    return Collections.emptyList();
  }

  @Override
  public Referable resolveName(String name) {
    return null;
  }
}
