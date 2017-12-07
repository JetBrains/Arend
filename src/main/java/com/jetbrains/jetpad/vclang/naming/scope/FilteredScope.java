package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class FilteredScope implements Scope {
  private final Scope myScope;
  private final Set<String> myNames;
  private final boolean myInclude;

  public FilteredScope(Scope scope, Set<String> names, boolean include) {
    myScope = scope;
    myNames = names;
    myInclude = include;
  }

  @Nonnull
  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>();
    for (Referable element : myScope.getElements()) {
      if (myInclude == myNames.contains(element.textRepresentation())) {
        elements.add(element);
      }
    }
    return elements;
  }

  @Override
  public Referable resolveName(String name) {
    if (myInclude) {
      return myNames.contains(name) ? myScope.resolveName(name) : null;
    } else {
      return myNames.contains(name) ? null : myScope.resolveName(name);
    }
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean resolveModuleNames) {
    if (myInclude) {
      return myNames.contains(name) ? myScope.resolveNamespace(name, resolveModuleNames) : null;
    } else {
      return myNames.contains(name) ? null : myScope.resolveNamespace(name, resolveModuleNames);
    }
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    return myScope.find(ref -> pred.test(ref) && myInclude == myNames.contains(ref.textRepresentation()));
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myScope.getImportedSubscope();
  }
}
