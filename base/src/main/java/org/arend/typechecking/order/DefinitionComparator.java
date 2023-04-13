package org.arend.typechecking.order;

import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefinitionComparator implements PartialComparator<Concrete.ResolvableDefinition> {
  private final PartialComparator<TCDefReferable> myComparator;

  DefinitionComparator(PartialComparator<TCDefReferable> comparator) {
    myComparator = comparator;
  }

  @NotNull
  @Override
  public Result compare(@Nullable Concrete.ResolvableDefinition def1, @Nullable Concrete.ResolvableDefinition def2) {
    if (def1 == def2) {
      return Result.EQUALS;
    }
    if (def1 == null || def2 == null) {
      return Result.UNCOMPARABLE;
    }
    return myComparator.compare(def1.getData(), def2.getData());
  }
}
