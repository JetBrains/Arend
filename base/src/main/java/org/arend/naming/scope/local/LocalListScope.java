package org.arend.naming.scope.local;

import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.DelegateScope;
import org.arend.naming.scope.Scope;

import java.util.List;
import java.util.function.Predicate;

public class LocalListScope extends DelegateScope {
  private final List<? extends ArendRef> myReferables;

  public LocalListScope(Scope parent, List<? extends ArendRef> referables) {
    super(parent);
    myReferables = referables;
  }

  private Referable findHere(Predicate<Referable> pred) {
    for (int i = myReferables.size() - 1; i >= 0; i--) {
      ArendRef referable = myReferables.get(i);
      if (referable instanceof Referable && pred.test((Referable) referable)) {
        return (Referable) referable;
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    Referable ref = findHere(pred);
    return ref != null ? ref : parent.find(pred);
  }
}
