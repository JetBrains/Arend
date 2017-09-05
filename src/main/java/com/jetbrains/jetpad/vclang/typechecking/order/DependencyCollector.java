package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.Typecheckable;

import java.util.*;

public class DependencyCollector<T> implements DependencyListener<T> {
  private final Map<GlobalReferable, Set<GlobalReferable>> myDependencies = new HashMap<>();
  private final Map<GlobalReferable, Set<GlobalReferable>> myReverseDependencies = new HashMap<>();
  private final TypecheckerState myState;

  public DependencyCollector(TypecheckerState state) {
    myState = state;
  }

  @Override
  public void dependsOn(Typecheckable<T> unit, Concrete.Definition<T> def) {
    myDependencies.computeIfAbsent(unit.getDefinition().getReferable(), k -> new HashSet<>()).add(def.getReferable());
    myReverseDependencies.computeIfAbsent(def.getReferable(), k -> new HashSet<>()).add(unit.getDefinition().getReferable());
  }

  public void update(GlobalReferable definition) {
    if (myState.getTypechecked(definition) != null) {
      return;
    }

    Set<GlobalReferable> updated = new HashSet<>();
    Stack<GlobalReferable> stack = new Stack<>();
    stack.push(definition);

    while (!stack.isEmpty()) {
      GlobalReferable toUpdate = stack.pop();
      if (!updated.add(toUpdate)) {
        continue;
      }

      Set<GlobalReferable> dependencies = myDependencies.remove(toUpdate);
      if (dependencies != null) {
        for (GlobalReferable dependency : dependencies) {
          Set<GlobalReferable> definitions = myReverseDependencies.get(dependency);
          if (definitions != null) {
            definitions.remove(definition);
          }
        }
      }

      Set<GlobalReferable> reverseDependencies = myReverseDependencies.remove(toUpdate);
      if (reverseDependencies != null) {
        stack.addAll(reverseDependencies);
      }
    }

    for (GlobalReferable updatedDef : updated) {
      myState.reset(updatedDef);
    }
  }
}
