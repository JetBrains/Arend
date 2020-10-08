package org.arend.ext.userData;

public class Key<T> {
  protected final String name;

  public Key(String name) {
    this.name = name;
  }

  public Key() {
    this.name = null;
  }

  public T copy(T value) {
    return value;
  }

  @Override
  public String toString() {
    return name != null ? name : super.toString();
  }
}
