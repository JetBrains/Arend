package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;

import java.util.Collection;
import java.util.List;

public class SCC<T> {
  private final List<TypecheckingUnit<T>> myUnits;

  public SCC(List<TypecheckingUnit<T>> units) {
    myUnits = units;
  }

  public Collection<? extends TypecheckingUnit<T>> getUnits() {
    return myUnits;
  }
}
