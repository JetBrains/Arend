package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.abs.Abstract;

import java.util.List;
import java.util.function.Predicate;

public class LetScope implements LocalScope {
  private final LocalScope myParent;
  private final List<? extends Abstract.LetClause> myClauses;

  LetScope(LocalScope parent, List<? extends Abstract.LetClause> clauses) {
    myParent = parent;
    myClauses = clauses;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (int i = myClauses.size() - 1; i >= 0; i--) {
      Referable ref = myClauses.get(i).getReferable();
      if (pred.test(ref)) {
        return ref;
      }
    }
    return myParent.find(pred);
  }

  @Override
  public Scope getGlobalScope() {
    return myParent.getGlobalScope();
  }
}
