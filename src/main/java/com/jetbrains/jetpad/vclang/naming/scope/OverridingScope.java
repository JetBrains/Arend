package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateDefinitionError;
import com.jetbrains.jetpad.vclang.term.Abstract;

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
    Set<String> intersection = new HashSet<>(parent.getNames());
    intersection.retainAll(child.getNames());
    for (String name : intersection) {
      errorReporter.report(new DuplicateDefinitionError(Error.Level.WARNING, parent.resolveName(name), child.resolveName(name)));
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
}
