package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;

import java.util.HashSet;
import java.util.Set;

public class SCC {
  public static class TypecheckingUnit {
    public Typecheckable typecheckable;
    public Abstract.ClassDefinition enclosingClass;

    public TypecheckingUnit(Typecheckable typecheckable, Abstract.ClassDefinition enclosingClass) {
      this.typecheckable = typecheckable;
      this.enclosingClass = enclosingClass;
    }
  }

  private final Set<TypecheckingUnit> myUnits = new HashSet<>();

  public void add(TypecheckingUnit unit) {
    myUnits.add(unit);
  }

  public Set<TypecheckingUnit> getUnits() {
    return myUnits;
  }
}
