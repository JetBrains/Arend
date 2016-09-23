package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

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
    ClassViewInstanceKey that = (ClassViewInstanceKey) o;
    return definition.equals(that.definition) && classView.equals(that.classView);

  }

  @Override
  public int hashCode() {
    return 31 * definition.hashCode() + classView.hashCode();
  }
}
