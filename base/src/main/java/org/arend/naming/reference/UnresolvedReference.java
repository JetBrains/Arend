package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;
import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface UnresolvedReference extends Referable, DataContainer {
  @NotNull Referable resolve(Scope scope, @Nullable List<Referable> resolvedRefs, @Nullable Scope.ScopeContext context);
  @Nullable Referable tryResolve(Scope scope, List<Referable> resolvedRefs);
  @Nullable Concrete.Expression resolveExpression(Scope scope, List<Referable> resolvedRefs);
  @Nullable Concrete.Expression tryResolveExpression(Scope scope, List<Referable> resolvedRefs);
  void reset();
  boolean isResolved();

  @NotNull
  default Referable resolve(Scope scope, List<Referable> resolvedRefs) {
    return resolve(scope, resolvedRefs, Scope.ScopeContext.STATIC);
  }

  @Override
  default boolean isLocalRef() {
    return false;
  }
}
