package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

public class SimpleClassViewInstanceProvider implements ClassViewInstanceProvider {
  private final Scope myScope;
  private Map<Abstract.ClassView, List<Abstract.ClassViewInstance>> myInstances = null;

  public SimpleClassViewInstanceProvider(Scope scope) {
    myScope = scope;
  }

  public Scope getScope() {
    return myScope;
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances(Abstract.ClassView classView) {
    if (myInstances == null) {
      myInstances = new HashMap<>();
      for (Abstract.ClassViewInstance instance : myScope.getInstances()) {
        myInstances.computeIfAbsent((Abstract.ClassView) instance.getClassView().getReferent(), k -> new ArrayList<>()).add(instance);
      }
    }
    return myInstances.getOrDefault(classView, Collections.emptyList());
  }
}
