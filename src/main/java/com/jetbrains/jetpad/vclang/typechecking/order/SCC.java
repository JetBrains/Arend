package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashSet;
import java.util.Set;

public class SCC {
  public static class TypecheckingUnit {
    public Abstract.Definition definition;
    public Abstract.ClassDefinition enclosingClass;

    public TypecheckingUnit(Abstract.Definition definition, Abstract.ClassDefinition enclosingClass) {
      this.definition = definition;
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
