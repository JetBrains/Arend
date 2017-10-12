package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SingletonScope implements Scope {
  private final Referable myReferable;

  public SingletonScope(Referable referable) {
    myReferable = referable;
  }

  @Override
  public List<Referable> getElements() {
    return Collections.singletonList(myReferable);
  }

  @Override
  public Referable resolveName(String name) {
    return myReferable.textRepresentation().equals(name) ? myReferable : null;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    return pred.test(myReferable) ? myReferable : null;
  }
}
