package org.arend.typechecking.order;

import org.arend.typechecking.typecheckable.TypecheckingUnit;

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
