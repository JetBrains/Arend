package com.jetbrains.jetpad.vclang.util;

public class Pair<T, S> {
  public final T proj1;
  public final S proj2;

  public Pair(T definition, S classView) {
    this.proj1 = definition;
    this.proj2 = classView;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pair<?, ?> that = (Pair<?, ?>) o;

    if (!proj1.equals(that.proj1)) return false;
    return proj2 != null ? proj2.equals(that.proj2) : that.proj2 == null;

  }

  @Override
  public int hashCode() {
    int result = proj1.hashCode();
    result = 31 * result + (proj2 != null ? proj2.hashCode() : 0);
    return result;
  }
}
