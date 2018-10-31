package org.arend.typechecking.order.dependency;

import org.arend.core.definition.*;
import org.arend.naming.reference.TCReferable;
import org.arend.typechecking.TypecheckerState;

import java.util.*;

public class DependencyCollector implements DependencyListener {
  private final Map<TCReferable, Set<TCReferable>> myDependencies = new HashMap<>();
  private final Map<TCReferable, Set<TCReferable>> myReverseDependencies = new HashMap<>();
  private final TypecheckerState myState;

  public DependencyCollector(TypecheckerState state) {
    myState = state;
  }

  @Override
  public void dependsOn(TCReferable def1, boolean header, TCReferable def2) {
    myDependencies.computeIfAbsent(def1, k -> new HashSet<>()).add(def2);
    myReverseDependencies.computeIfAbsent(def2, k -> new HashSet<>()).add(def1);
  }

  @Override
  public Set<? extends TCReferable> update(TCReferable definition) {
    if (myState.getTypechecked(definition) == null) {
      return Collections.emptySet();
    }

    Set<TCReferable> updated = new HashSet<>();
    Stack<TCReferable> stack = new Stack<>();
    stack.push(definition);

    while (!stack.isEmpty()) {
      TCReferable toUpdate = stack.pop();
      if (!updated.add(toUpdate)) {
        continue;
      }

      Set<TCReferable> dependencies = myDependencies.remove(toUpdate);
      if (dependencies != null) {
        for (TCReferable dependency : dependencies) {
          Set<TCReferable> definitions = myReverseDependencies.get(dependency);
          if (definitions != null) {
            definitions.remove(definition);
          }
        }
      }

      Set<TCReferable> reverseDependencies = myReverseDependencies.remove(toUpdate);
      if (reverseDependencies != null) {
        stack.addAll(reverseDependencies);
      }
    }

    for (TCReferable updatedDef : updated) {
      Definition def = myState.reset(updatedDef);
      if (def instanceof ClassDefinition) {
        for (ClassField field : ((ClassDefinition) def).getPersonalFields()) {
          myState.reset(field.getReferable());
        }
      } else if (def instanceof DataDefinition) {
        for (Constructor constructor : ((DataDefinition) def).getConstructors()) {
          myState.reset(constructor.getReferable());
        }
      }
    }

    return updated;
  }
}
