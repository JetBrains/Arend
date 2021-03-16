package org.arend.util;

public class Wrapper<T> {
  public final T element;

  public Wrapper(T element) {
    this.element = element;
  }

  @Override
  public int hashCode() {
    return element.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Wrapper && element == ((Wrapper<?>) obj).element;
  }
}
