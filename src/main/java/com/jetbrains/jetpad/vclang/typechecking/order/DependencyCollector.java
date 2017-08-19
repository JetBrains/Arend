package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import java.util.*;

public class DependencyCollector<T> implements DependencyListener<T> {
  private final Map<Abstract.Definition, Set<Abstract.Definition>> myDependencies = new HashMap<>();
  private final Map<Abstract.Definition, Set<Abstract.Definition>> myReverseDependencies = new HashMap<>();
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
    Set<Abstract.Definition> updated = new HashSet<>();
    Stack<Abstract.Definition> stack = new Stack<>();
    stack.push(definition);

    while (!stack.isEmpty()) {
      Abstract.Definition toUpdate = stack.pop();
      if (!updated.add(toUpdate)) {
        continue;
      }

      Set<Abstract.Definition> dependencies = myDependencies.remove(toUpdate);
      if (dependencies != null) {
        for (Abstract.Definition dependency : dependencies) {
          Set<Abstract.Definition> definitions = myReverseDependencies.get(dependency);
          if (definitions != null) {
            definitions.remove(definition);
          }
        }
      }

      Set<Abstract.Definition> reverseDependencies = myReverseDependencies.remove(toUpdate);
      if (reverseDependencies != null) {
        stack.addAll(reverseDependencies);
      }
    }

    for (Abstract.Definition updatedDef : updated) {
      myState.reset(updatedDef);
    }
  }
}
