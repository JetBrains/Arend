package com.jetbrains.jetpad.vclang.typechecking.typeclass.scope;

import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

public class SimpleInstanceScope implements Scope {
  private List<Abstract.ClassViewInstance> myInstances = Collections.emptyList();

  public void addInstance(Abstract.ClassViewInstance instance) {
    if (myInstances.isEmpty()) {
      myInstances = new ArrayList<>();
    }
    myInstances.add(instance);
  }

  public void addAll(SimpleInstanceScope other) {
    if (!other.myInstances.isEmpty() && myInstances.isEmpty()) {
      myInstances = new ArrayList<>();
    }
    for (Abstract.ClassViewInstance instance : other.myInstances) {
      myInstances.add(instance);
    }
  }

  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    return null;
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    return myInstances;
  }
}
