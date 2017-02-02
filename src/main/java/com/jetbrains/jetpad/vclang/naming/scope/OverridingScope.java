package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateInstanceError;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

public class OverridingScope implements Scope {
  private final Scope myParent;
  private final Scope myChild;

  public OverridingScope(Scope parent, Scope child) {
    myParent = parent;
    myChild = child;
  }

  public static OverridingScope mergeInstances(Scope parent, Scope child, ErrorReporter errorReporter) {
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
    List<Abstract.ClassViewInstance> instances = new ArrayList<>(myParent.getInstances());
    instances.addAll(myChild.getInstances());
    return instances;
  }
}
