package org.arend.typechecking.order.dependency;

import org.arend.core.definition.*;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.DefinableMetaDefinition;
import org.arend.typechecking.visitor.CollectDefCallsVisitor;

import java.util.*;

public class DependencyCollector implements DependencyListener {
  private final Map<TCReferable, Set<TCReferable>> myDependencies = new HashMap<>();
  private final Map<TCReferable, Set<TCReferable>> myReverseDependencies = new HashMap<>();

  @Override
  public void dependsOn(TCReferable def1, TCReferable def2) {
    ModuleLocation location = def2.getLocation();
    if (location != null && location.isExternalLibrary()) {
      return;
    }

    if (!(def1 instanceof MetaDefinition)) {
      myDependencies.computeIfAbsent(def1, k -> new HashSet<>()).add(def2);
    }
    myReverseDependencies.computeIfAbsent(def2, k -> new HashSet<>()).add(def1);
  }

  @Override
  public Set<? extends TCReferable> update(TCReferable definition) {
    if (definition instanceof TCDefReferable && ((TCDefReferable) definition).getTypechecked() == null) {
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

      Set<TCReferable> dependencies;
      if (toUpdate instanceof MetaReferable) {
        MetaDefinition metaDef = ((MetaReferable) toUpdate).getDefinition();
        if (metaDef instanceof DefinableMetaDefinition) {
          dependencies = new HashSet<>();
          ((DefinableMetaDefinition) metaDef).accept(new CollectDefCallsVisitor(dependencies, true), null);
        } else {
          dependencies = null;
        }
      } else {
        dependencies = myDependencies.remove(toUpdate);
      }

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

    Set<TCReferable> additional = new HashSet<>();
    for (TCReferable updatedDef : updated) {
      if (!(updatedDef instanceof TCDefReferable)) {
        continue;
      }
      Definition def = ((TCDefReferable) updatedDef).getTypechecked();
      ((TCDefReferable) updatedDef).dropAndCancelTypechecking();
      if (def instanceof ClassDefinition) {
        for (ClassField field : ((ClassDefinition) def).getPersonalFields()) {
          field.getReferable().dropAndCancelTypechecking();
          additional.add(field.getReferable());
        }
      } else if (def instanceof DataDefinition) {
        for (Constructor constructor : ((DataDefinition) def).getConstructors()) {
          constructor.getReferable().dropAndCancelTypechecking();
          additional.add(constructor.getReferable());
        }
      }
    }

    updated.addAll(additional);
    return updated;
  }

  @Override
  public Set<? extends TCReferable> getDependencies(TCReferable definition) {
    Set<TCReferable> dependencies = myDependencies.get(definition);
    return dependencies == null ? Collections.emptySet() : dependencies;
  }
}
