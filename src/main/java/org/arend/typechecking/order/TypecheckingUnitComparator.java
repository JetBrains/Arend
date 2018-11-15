package org.arend.typechecking.order;

import org.arend.naming.reference.TCReferable;
import org.arend.typechecking.typecheckable.TypecheckingUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TypecheckingUnitComparator implements PartialComparator<TypecheckingUnit> {
  private final PartialComparator<TCReferable> myComparator;

  TypecheckingUnitComparator(PartialComparator<TCReferable> comparator) {
    myComparator = comparator;
  }

  @Nonnull
  @Override
  public Result compare(@Nullable TypecheckingUnit t1, @Nullable TypecheckingUnit t2) {
    if (t1 == t2) {
      return Result.EQUALS;
    }
    if (t1 == null || t2 == null) {
      return Result.UNCOMPARABLE;
    }
    return myComparator.compare(t1.getDefinition().getData(), t2.getDefinition().getData());
  }
}
