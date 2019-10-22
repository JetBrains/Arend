package org.arend.typechecking.order;

import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DefinitionComparator implements PartialComparator<Concrete.Definition> {
  private final PartialComparator<TCReferable> myComparator;

  DefinitionComparator(PartialComparator<TCReferable> comparator) {
    myComparator = comparator;
  }

  @Nonnull
  @Override
  public Result compare(@Nullable Concrete.Definition def1, @Nullable Concrete.Definition def2) {
    if (def1 == def2) {
      return Result.EQUALS;
    }
    if (def1 == null || def2 == null) {
      return Result.UNCOMPARABLE;
    }
    return myComparator.compare(def1.getData(), def2.getData());
  }
}
