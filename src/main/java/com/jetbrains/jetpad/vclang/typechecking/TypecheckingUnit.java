package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class TypecheckingUnit {
  private final Typecheckable myTypecheckable;
  private final Abstract.ClassDefinition myEnclosingClass;

  public TypecheckingUnit(Typecheckable typecheckable, Abstract.ClassDefinition enclosingClass) {
    this.myTypecheckable = typecheckable;
    this.myEnclosingClass = enclosingClass;
  }

  public Typecheckable getTypecheckable() {
    return myTypecheckable;
  }

  public Abstract.Definition getDefinition() {
    return myTypecheckable.getDefinition();
  }

  public boolean isHeader() {
    return myTypecheckable.isHeader();
  }

  public Abstract.ClassDefinition getEnclosingClass() {
    return myEnclosingClass;
  }
}
