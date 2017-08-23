package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.*;

public class SimpleInstanceProvider implements InstanceProvider {
  private final Scope myScope;
  private Map<Concrete.ClassView, List<Concrete.Instance>> myInstances = null;

  public SimpleInstanceProvider(Scope scope) {
    myScope = scope;
  }

  public Scope getScope() {
    return myScope;
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances(Concrete.ClassView classView) {
    if (myInstances == null) {
      myInstances = new HashMap<>();
      for (Concrete.Instance instance : myScope.getInstances()) {
        myInstances.computeIfAbsent((Concrete.ClassView) instance.getClassView().getReferent(), k -> new ArrayList<>()).add(instance);
      }
    }
    return myInstances.getOrDefault(classView, Collections.emptyList());
  }
}
