package com.jetbrains.jetpad.vclang.naming.scope;

import com.google.common.base.Objects;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.*;

public class LocalScope implements Scope {
  private final List<Referable> myContext;

  public LocalScope(List<Referable> context) {
    myContext = context;
  }

  @Override
  public List<Referable> getElements() {
    return myContext;
  }

  @Override
  public Referable resolveName(String name) {
    for (int i = myContext.size() - 1; i >= 0; i--) {
      if (Objects.equal(myContext.get(i).textRepresentation(), name)) {
        return myContext.get(i);
      }
    }
    return null;
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    return Collections.emptyList();
  }
}
