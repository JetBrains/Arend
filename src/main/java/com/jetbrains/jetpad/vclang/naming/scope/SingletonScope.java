package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class SingletonScope implements Scope {
  private final Referable myReferable;

  public SingletonScope(Referable referable) {
    myReferable = referable;
  }

  @Override
  public Set<String> getNames() {
    return Collections.singleton(myReferable.textRepresentation());
  }

  @Override
  public Referable resolveName(String name) {
    return myReferable.textRepresentation().equals(name) ? myReferable : null;
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    return Collections.emptyList();
  }
}
