package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import java.util.*;

public class DependencyCollector implements DependencyListener {
  private final Map<GlobalReferable, Set<GlobalReferable>> myDependencies = new HashMap<>();
  private final Map<GlobalReferable, Set<GlobalReferable>> myReverseDependencies = new HashMap<>();
  private final TypecheckerState myState;

  public DependencyCollector(TypecheckerState state) {
    myState = state;
  }

  @Override
  public void dependsOn(GlobalReferable def1, boolean header, GlobalReferable def2) {
    myDependencies.computeIfAbsent(def1, k -> new HashSet<>()).add(def2);
    myReverseDependencies.computeIfAbsent(def2, k -> new HashSet<>()).add(def1);
  }

  public Set<? extends GlobalReferable> update(GlobalReferable definition) {
    if (myState.getTypechecked(definition) == null) {
      return Collections.emptySet();
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

    return updated;
  }
}
