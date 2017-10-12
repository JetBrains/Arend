package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.abs.Abstract;

import java.util.List;
import java.util.function.Predicate;

public class TelescopeScope implements LocalScope {
  private final LocalScope myParent;
  private final List<? extends Abstract.Parameter> myParameters;

  TelescopeScope(LocalScope parent, List<? extends Abstract.Parameter> parameters) {
    myParent = parent;
    myParameters = parameters;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (int i = myParameters.size() - 1; i >= 0; i--) {
      List<? extends Referable> referables = myParameters.get(i).getReferableList();
      for (int j = referables.size() - 1; j >= 0; j--) {
        if (referables.get(j) != null && pred.test(referables.get(j))) {
          return referables.get(j);
        }
      }
    }
    return myParent.find(pred);
  }

  @Override
  public Scope getGlobalScope() {
    return myParent.getGlobalScope();
  }
}
