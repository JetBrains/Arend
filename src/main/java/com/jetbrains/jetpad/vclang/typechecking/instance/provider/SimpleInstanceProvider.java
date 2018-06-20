package com.jetbrains.jetpad.vclang.typechecking.instance.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.*;

public class SimpleInstanceProvider implements InstanceProvider {
  private Map<ClassReferable, List<Concrete.Instance>> myInstances = new HashMap<>();

  public SimpleInstanceProvider() {
  }

  public SimpleInstanceProvider(SimpleInstanceProvider another) {
    myInstances = new HashMap<>();
    for (Map.Entry<ClassReferable, List<Concrete.Instance>> entry : another.myInstances.entrySet()) {
      myInstances.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
  }

  public void put(ClassReferable classRef, Concrete.Instance instance) {
    myInstances.computeIfAbsent(classRef, ref -> new ArrayList<>()).add(instance);
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances(ClassReferable classRef) {
    return myInstances.getOrDefault(classRef, Collections.emptyList());
  }
}
