package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import java.util.*;

public class DependencyCollector<T> implements DependencyListener<T> {
  private final Map<Concrete.Definition<T>, Set<Concrete.Definition<T>>> myDependencies = new HashMap<>();
  private final Map<Concrete.Definition<T>, Set<Concrete.Definition<T>>> myReverseDependencies = new HashMap<>();
  private final TypecheckerState myState;

  public DependencyCollector(TypecheckerState state) {
    myState = state;
  }

  @Override
  public void dependsOn(Typecheckable<T> unit, Concrete.Definition<T> def) {
    myDependencies.computeIfAbsent(unit.getDefinition(), k -> new HashSet<>()).add(def);
    myReverseDependencies.computeIfAbsent(def, k -> new HashSet<>()).add(unit.getDefinition());
  }

  public void update(Concrete.Definition<T> definition) {
    Set<Concrete.Definition<T>> updated = new HashSet<>();
    Stack<Concrete.Definition<T>> stack = new Stack<>();
    stack.push(definition);

    while (!stack.isEmpty()) {
      Concrete.Definition<T> toUpdate = stack.pop();
      if (!updated.add(toUpdate)) {
        continue;
      }

      Set<Concrete.Definition<T>> dependencies = myDependencies.remove(toUpdate);
      if (dependencies != null) {
        for (Concrete.Definition<T> dependency : dependencies) {
          Set<Concrete.Definition<T>> definitions = myReverseDependencies.get(dependency);
          if (definitions != null) {
            definitions.remove(definition);
          }
        }
      }

      Set<Concrete.Definition<T>> reverseDependencies = myReverseDependencies.remove(toUpdate);
      if (reverseDependencies != null) {
        stack.addAll(reverseDependencies);
      }
    }

    for (Concrete.Definition<T> updatedDef : updated) {
      myState.reset(updatedDef);
    }
  }
}
