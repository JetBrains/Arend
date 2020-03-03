package org.arend.naming.reference;

import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NamedUnresolvedReference implements UnresolvedReference {
  private final Object myData;
  private final String myName;
  private Referable resolved;

  public NamedUnresolvedReference(Object data, @NotNull String name) {
    myData = data;
    myName = name;
  }

  @Nullable
  @Override
  public Object getData() {
    return myData;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NamedUnresolvedReference that = (NamedUnresolvedReference) o;
    return myName.equals(that.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }

  @NotNull
  @Override
  public Referable resolve(Scope scope, List<Referable> resolvedRefs) {
    if (resolved != null) {
      return resolved;
    }

    resolved = scope.resolveName(myName);
    if (resolved == null) {
      resolved = new ErrorReference(myData, myName);
    }
    if (resolvedRefs != null) {
      resolvedRefs.add(resolved);
    }
    return resolved;
  }

  @Nullable
  @Override
  public Referable tryResolve(Scope scope) {
    if (resolve(scope, null) instanceof ErrorReference) {
      resolved = null;
    }
    return resolved;
  }

  @Nullable
  @Override
  public Concrete.Expression resolveArgument(Scope scope, List<Referable> resolvedRefs) {
    resolve(scope, resolvedRefs);
    return null;
  }
}
