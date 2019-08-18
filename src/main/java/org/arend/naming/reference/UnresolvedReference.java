package org.arend.naming.reference;

import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface UnresolvedReference extends Referable, DataContainer {
  @Nonnull Referable resolve(Scope scope, List<Referable> resolvedRefs);
  @Nullable Referable tryResolve(Scope scope);
  @Nullable Concrete.Expression resolveArgument(Scope scope, List<Referable> resolvedRefs);
}
