package org.arend.typechecking.order;

import org.arend.naming.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.TCDefReferable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ConcreteIndexComparator implements PartialComparator<TCDefReferable> {
  public static ConcreteIndexComparator INSTANCE = new ConcreteIndexComparator();

  private ConcreteIndexComparator() {}

  @Override
  public @NotNull Result compare(@Nullable TCDefReferable t1, @Nullable TCDefReferable t2) {
    if (t1 == t2) {
      return Result.EQUALS;
    }

    if (!(t1 instanceof ConcreteLocatedReferable ref1 && t2 instanceof ConcreteLocatedReferable ref2 && ref1.index >= 0 && ref2.index >= 0 && Objects.equals(t1.getLocation(), t2.getLocation()))) {
      return Result.UNCOMPARABLE;
    }

    return ref1.index == ref2.index ? Result.EQUALS : ref1.index < ref2.index ? Result.LESS : Result.GREATER;
  }
}
