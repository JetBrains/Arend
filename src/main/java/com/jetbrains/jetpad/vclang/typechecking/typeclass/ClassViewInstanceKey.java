package com.jetbrains.jetpad.vclang.typechecking.typeclass;

public class ClassViewInstanceKey<D, T> {
  public final D definition;
  public final T classView;

  public ClassViewInstanceKey(D definition, T classView) {
    this.definition = definition;
    this.classView = classView;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClassViewInstanceKey<?, ?> that = (ClassViewInstanceKey<?, ?>) o;

    if (!definition.equals(that.definition)) return false;
    return classView != null ? classView.equals(that.classView) : that.classView == null;

  }

  @Override
  public int hashCode() {
    int result = definition.hashCode();
    result = 31 * result + (classView != null ? classView.hashCode() : 0);
    return result;
  }
}
