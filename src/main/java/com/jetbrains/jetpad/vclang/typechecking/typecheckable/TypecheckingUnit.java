package com.jetbrains.jetpad.vclang.typechecking.typecheckable;

import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

public class TypecheckingUnit {
  private final Typecheckable myTypecheckable;
  private final Concrete.ClassDefinition myEnclosingClass;

  public TypecheckingUnit(Typecheckable typecheckable, Concrete.ClassDefinition enclosingClass) {
    this.myTypecheckable = typecheckable;
    this.myEnclosingClass = enclosingClass;
  }

  public Typecheckable getTypecheckable() {
    return myTypecheckable;
  }

  public Concrete.Definition getDefinition() {
    return myTypecheckable.getDefinition();
  }

  public boolean isHeader() {
    return myTypecheckable.isHeader();
  }

  public Concrete.ClassDefinition getEnclosingClass() {
    return myEnclosingClass;
  }
}
