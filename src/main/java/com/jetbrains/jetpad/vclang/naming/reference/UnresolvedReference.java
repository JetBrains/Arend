package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nonnull;

public class UnresolvedReference implements Referable {
  private final String myName;

  public UnresolvedReference(@Nonnull String name) {
    myName = name;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UnresolvedReference that = (UnresolvedReference) o;
    return myName.equals(that.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
