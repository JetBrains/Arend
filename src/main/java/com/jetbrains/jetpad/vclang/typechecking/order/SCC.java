package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;

import java.util.HashSet;
import java.util.Set;

public class SCC {
  private final Set<TypecheckingUnit> myUnits = new HashSet<>();

  public void add(TypecheckingUnit unit) {
    myUnits.add(unit);
  }

  public Set<TypecheckingUnit> getUnits() {
    return myUnits;
  }
}
