package org.arend.ext.userData;

@SuppressWarnings("unused")
public class Key<T> {
  protected final String name;

  public Key(String name) {
    this.name = name;
  }

  public Key() {
    this.name = null;
  }

  @Override
  public String toString() {
    return name != null ? name : super.toString();
  }
}
