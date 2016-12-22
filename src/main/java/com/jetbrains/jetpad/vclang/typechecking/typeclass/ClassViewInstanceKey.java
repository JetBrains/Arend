package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.core.definition.Definition;

public class ClassViewInstanceKey<T> {
  public final Definition definition;
  public final T classView;

  public ClassViewInstanceKey(Definition definition, T classView) {
    this.definition = definition;
    this.classView = classView;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClassViewInstanceKey<?> that = (ClassViewInstanceKey<?>) o;

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
