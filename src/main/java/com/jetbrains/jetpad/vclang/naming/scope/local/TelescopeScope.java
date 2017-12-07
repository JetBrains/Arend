package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.ImportedScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.abs.Abstract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class TelescopeScope implements Scope {
  private final Scope myParent;
  private final List<? extends Abstract.Parameter> myParameters;
  private final Collection<? extends Referable> myExcluded;

  public TelescopeScope(Scope parent, List<? extends Abstract.Parameter> parameters) {
    myParent = parent;
    myParameters = parameters;
    myExcluded = Collections.emptyList();
  }

  public TelescopeScope(Scope parent, List<? extends Abstract.Parameter> parameters, Collection<? extends Referable> excluded) {
    myParent = parent;
    myParameters = parameters;
    myExcluded = excluded;
  }

  private Referable findHere(Predicate<Referable> pred) {
    for (int i = myParameters.size() - 1; i >= 0; i--) {
      List<? extends Referable> referables = myParameters.get(i).getReferableList();
      for (int j = referables.size() - 1; j >= 0; j--) {
        if (referables.get(j) != null && !myExcluded.contains(referables.get(j)) && pred.test(referables.get(j))) {
          return referables.get(j);
        }
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    Referable ref = findHere(pred);
    return ref != null ? ref : myParent.find(pred);
  }

  @Override
  public Referable resolveName(String name) {
    Referable ref = findHere(ref2 -> ref2.textRepresentation().equals(name));
    return ref != null ? ref : myParent.resolveName(name);
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscope() {
    return myParent.getGlobalSubscope();
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
