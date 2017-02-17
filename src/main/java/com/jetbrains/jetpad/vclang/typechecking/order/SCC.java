package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;

import java.util.Collection;
import java.util.List;

public class SCC {
  private final List<TypecheckingUnit> myUnits;

  public SCC(List<TypecheckingUnit> units) {
    myUnits = units;
  }

  public Collection<? extends TypecheckingUnit> getUnits() {
    return myUnits;
  }
}
