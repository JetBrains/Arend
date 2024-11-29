package org.arend.naming.scope.local;

import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.DelegateScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

public class ElimScope extends DelegateScope {
  private final Set<? extends ArendRef> myExcluded;

  public ElimScope(Scope parent, Set<? extends ArendRef> excluded) {
    super(parent);
    myExcluded = excluded;
  }

  @Override
  public Referable find(Predicate<Referable> pred, @Nullable ScopeContext context) {
    return parent.find(ref -> !myExcluded.contains(ref) && pred.test(ref), context);
  }

  @Override
  public Referable resolveName(@NotNull String name, @Nullable ScopeContext context) {
    Referable ref = parent.resolveName(name, context);
    return ref != null && myExcluded.contains(ref) ? null : ref;
  }
}
