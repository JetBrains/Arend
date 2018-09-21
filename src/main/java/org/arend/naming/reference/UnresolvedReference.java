package org.arend.naming.reference;

import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface UnresolvedReference extends Referable, DataContainer {
  @Nonnull Referable resolve(Scope scope);
  @Nullable Referable tryResolve(Scope scope);
  @Nullable Concrete.Expression resolveArgument(Scope scope);
}
