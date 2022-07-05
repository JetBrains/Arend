package org.arend.naming.scope.local;

import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.DelegateScope;

import java.util.Set;
import java.util.function.Predicate;

public class ElimScope extends DelegateScope {
  private final Set<? extends ArendRef> myExcluded;

  public ElimScope(Scope parent, Set<? extends ArendRef> excluded) {
    super(parent);
    myExcluded = excluded;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    return parent.find(ref -> !myExcluded.contains(ref) && pred.test(ref));
  }

  @Override
  public Referable resolveName(String name) {
    Referable ref = parent.resolveName(name);
    return ref != null && myExcluded.contains(ref) ? null : ref;
  }
}
