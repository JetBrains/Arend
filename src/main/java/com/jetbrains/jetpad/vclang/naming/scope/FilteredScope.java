package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

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
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>();
    for (Referable element : myScope.getElements()) {
      if (myInclude == myNames.contains(element.textRepresentation())) {
        elements.add(element);
      }
    }
    return elements;
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
      if (myInclude && myNames.contains(instance.getData().textRepresentation()) || !myInclude && !myNames.contains(instance.getData().textRepresentation())) {
        filteredInstances.add(instance);
      }
    }
    return filteredInstances;
  }
}
