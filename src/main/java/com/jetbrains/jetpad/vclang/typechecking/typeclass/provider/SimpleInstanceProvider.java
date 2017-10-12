package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class SimpleInstanceProvider implements InstanceProvider {
  private final Scope myScope;
  private Map<GlobalReferable, List<Concrete.Instance>> myInstances = null;
  private final ConcreteProvider myConcreteProvider;

  public SimpleInstanceProvider(Scope scope, ConcreteProvider concreteProvider) {
    myScope = scope;
    myConcreteProvider = concreteProvider;
  }

  public Scope getScope() {
    return myScope;
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances(Concrete.ClassView classView) {
    if (myInstances == null) {
      myInstances = new HashMap<>();
      for (Referable referable : myScope.getElements()) {
        if (referable instanceof GlobalReferable) {
          Concrete.ReferableDefinition definition = myConcreteProvider.getConcrete((GlobalReferable) referable);
          if (definition instanceof Concrete.Instance) {
            Concrete.Instance instance = (Concrete.Instance) definition;
            if (instance.getClassView().getReferent() instanceof GlobalReferable) {
              myInstances.computeIfAbsent((GlobalReferable) instance.getClassView().getReferent(), k -> new ArrayList<>()).add(instance);
            }
          }
        }
      }
    }
    return myInstances.getOrDefault(classView.getData(), Collections.emptyList());
  }
}
