package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.*;

public class FilteredScope implements Scope {
  private final Scope myScope;
  private final Set<String> myNames;
  private final boolean myInclude;

  public FilteredScope(Scope scope, Set<String> names, boolean include) {
    myScope = scope;
    myNames = names;
    myInclude = include;
  }

  @Override
  public Set<String> getNames() {
    Set<String> names = new HashSet<>(myScope.getNames());
    if (myInclude) {
      names.retainAll(myNames);
    } else {
      names.removeAll(myNames);
    }
    return names;
  }

  @Override
  public Referable resolveName(String name) {
    if (myInclude) {
      return myNames.contains(name) ? myScope.resolveName(name) : null;
    } else {
      return myNames.contains(name) ? null : myScope.resolveName(name);
    }
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    Collection<? extends Concrete.Instance> instances = myScope.getInstances();
    List<Concrete.Instance> filteredInstances = new ArrayList<>(instances.size());
    for (Concrete.Instance instance : instances) {
      if (myInclude && myNames.contains(instance.getName()) || !myInclude && !myNames.contains(instance.getName())) {
        filteredInstances.add(instance);
      }
    }
    return filteredInstances;
  }
}
