package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Concrete;

public class TypecheckingUnit<T> {
  private final Typecheckable<T> myTypecheckable;
  private final Concrete.ClassDefinition<T> myEnclosingClass;

  public TypecheckingUnit(Typecheckable<T> typecheckable, Concrete.ClassDefinition<T> enclosingClass) {
    this.myTypecheckable = typecheckable;
    this.myEnclosingClass = enclosingClass;
  }

  public Typecheckable<T> getTypecheckable() {
    return myTypecheckable;
  }

  public Concrete.Definition<T> getDefinition() {
    return myTypecheckable.getDefinition();
  }

  public boolean isHeader() {
    return myTypecheckable.isHeader();
  }

  public Concrete.ClassDefinition<T> getEnclosingClass() {
    return myEnclosingClass;
  }
}
