package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateDefinitionError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateInstanceError;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class OverridingScope implements Scope {
  private final Scope myParent;
  private final Scope myChild;

  public OverridingScope(Scope parent, Scope child) {
    myParent = parent;
    myChild = child;
  }

  public static OverridingScope merge(Scope parent, Scope child, ErrorReporter errorReporter) {
    Set<String> parentNames = parent.getNames();
    if (!parentNames.isEmpty()) {
      Set<String> childNames = child.getNames();
      if (!childNames.isEmpty()) {
        for (String name : parentNames) {
          if (childNames.contains(name)) {
            errorReporter.report(new DuplicateDefinitionError(Error.Level.WARNING, parent.resolveName(name), child.resolveName(name)));
          }
        }
      }
    }

    Collection<? extends Abstract.ClassViewInstance> parentInstances = parent.getInstances();
    if (!parentInstances.isEmpty()) {
      Collection<? extends Abstract.ClassViewInstance> childInstances = child.getInstances();
      if (!childInstances.isEmpty()) {
        for (Abstract.ClassViewInstance instance : parentInstances) {
          for (Abstract.ClassViewInstance childInstance : childInstances) {
            if (instance.getClassView().getReferent() == childInstance.getClassView().getReferent() && instance.getClassifyingDefinition() == childInstance.getClassifyingDefinition()) {
              errorReporter.report(new DuplicateInstanceError(Error.Level.WARNING, instance, childInstance));
            }
          }
        }
      }
    }

    return new OverridingScope(parent, child);
  }

  @Override
  public Set<String> getNames() {
    Set<String> names = new HashSet<>(myParent.getNames());
    names.addAll(myChild.getNames());
    return names;
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    Abstract.Definition ref = myChild.resolveName(name);
    return ref != null ? ref : myParent.resolveName(name);
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    Set<Abstract.ClassViewInstance> instances = new HashSet<>(myParent.getInstances());
    instances.addAll(myChild.getInstances());
    return instances;
  }

  @Override
  public Abstract.ClassViewInstance resolveInstance(Abstract.ClassView classView, Abstract.Definition classifyingDefinition) {
    Abstract.ClassViewInstance instance = myChild.resolveInstance(classView, classifyingDefinition);
    return instance != null ? instance : myParent.resolveInstance(classView, classifyingDefinition);
  }

  @Override
  public Abstract.ClassViewInstance resolveInstance(Abstract.ClassDefinition classDefinition, Abstract.Definition classifyingDefinition) {
    Abstract.ClassViewInstance instance = myChild.resolveInstance(classDefinition, classifyingDefinition);
    return instance != null ? instance : myParent.resolveInstance(classDefinition, classifyingDefinition);
  }
}
