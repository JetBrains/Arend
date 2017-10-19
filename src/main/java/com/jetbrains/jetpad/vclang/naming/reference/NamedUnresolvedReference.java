package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NamedUnresolvedReference implements UnresolvedReference {
  private final Object myData;
  private final String myName;
  protected Referable resolved;

  public NamedUnresolvedReference(Object data, @Nonnull String name) {
    myData = data;
    myName = name;
  }

  @Nullable
  @Override
  public Object getData() {
    return myData;
  }

  @Nonnull
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

  @Nullable
  @Override
  public Referable resolve(Scope scope, NameResolver nameResolver) {
    if (resolved != null) {
      return resolved;
    }

    resolved = scope.resolveName(myName);
    if (resolved == null) {
      resolved = new ErrorReference(myData, null, myName);
    }
    return resolved;
  }
}
