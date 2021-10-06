package org.arend.ext.util;

import java.util.Objects;

public class Pair<T, S> {
  public final T proj1;
  public final S proj2;

  public Pair(T proj1, S proj2) {
    this.proj1 = proj1;
    this.proj2 = proj2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pair<?, ?> pair = (Pair<?, ?>) o;

    if (!Objects.equals(proj1, pair.proj1)) return false;
    return Objects.equals(proj2, pair.proj2);
  }

  @Override
  public int hashCode() {
    int result = proj1 != null ? proj1.hashCode() : 0;
    result = 31 * result + (proj2 != null ? proj2.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "(" + (proj1 == null ? "null" : proj1) + ", " + (proj2 == null ? "null" : proj2) + ')';
  }
}
