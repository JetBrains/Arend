package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.*;

public class SimpleInstanceProvider implements InstanceProvider {
  private final Scope myScope;
  private Map<GlobalReferable, List<Concrete.Instance>> myInstances = null;

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
        if (instance.getClassView().getReferent() instanceof GlobalReferable) {
          myInstances.computeIfAbsent((GlobalReferable) instance.getClassView().getReferent(), k -> new ArrayList<>()).add(instance);
        }
      }
    }
    return myInstances.getOrDefault(classView.getReferable(), Collections.emptyList());
  }
}
