package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;

public class NamedUnresolvedReference implements UnresolvedReference {
  private final String myName;
  protected Referable resolved;

  public NamedUnresolvedReference(@Nonnull String name) {
    myName = name;
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

  @Nonnull
  @Override
  public Referable resolve(Scope scope, NameResolver nameResolver) {
    if (resolved != null) {
      return resolved;
    }

    if (scope != null) {
      resolved = scope.resolveName(myName);
    }
    if (resolved == null) {
      resolved = this;
    }
    return resolved;
  }
}
