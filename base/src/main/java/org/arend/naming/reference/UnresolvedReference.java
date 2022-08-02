package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;
import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface UnresolvedReference extends Referable, DataContainer {
  @NotNull Referable resolve(Scope scope, @Nullable List<Referable> resolvedRefs, @Nullable RefKind kind);
  @Nullable Referable tryResolve(Scope scope, List<Referable> resolvedRefs);
  @Nullable Concrete.Expression resolveArgument(Scope scope, List<Referable> resolvedRefs);
  void reset();

  @NotNull
  default Referable resolve(Scope scope, List<Referable> resolvedRefs) {
    return resolve(scope, resolvedRefs, RefKind.EXPR);
  }

  @Override
  default boolean isLocalRef() {
    return false;
  }
}
