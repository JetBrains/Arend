package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.abs.Abstract;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class TelescopeScope implements Scope {
  private final Scope myParent;
  private final List<? extends Abstract.Parameter> myParameters;
  private final Collection<? extends Referable> myExcluded;

  TelescopeScope(Scope parent, List<? extends Abstract.Parameter> parameters) {
    myParent = parent;
    myParameters = parameters;
    myExcluded = Collections.emptyList();
  }

  TelescopeScope(Scope parent, List<? extends Abstract.Parameter> parameters, Collection<? extends Referable> excluded) {
    myParent = parent;
    myParameters = parameters;
    myExcluded = excluded;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (int i = myParameters.size() - 1; i >= 0; i--) {
      List<? extends Referable> referables = myParameters.get(i).getReferableList();
      for (int j = referables.size() - 1; j >= 0; j--) {
        if (referables.get(j) != null && !myExcluded.contains(referables.get(j)) && pred.test(referables.get(j))) {
          return referables.get(j);
        }
      }
    }
    return myParent.find(pred);
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscope() {
    return myParent.getGlobalSubscope();
  }
}
