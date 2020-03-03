package org.arend.frontend;

import org.arend.frontend.parser.Position;
import org.arend.naming.reference.TCReferable;
import org.arend.typechecking.order.PartialComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PositionComparator implements PartialComparator<TCReferable> {
  public final static PositionComparator INSTANCE = new PositionComparator();

  private PositionComparator() {}

  @NotNull
  @Override
  public Result compare(@Nullable TCReferable t1, @Nullable TCReferable t2) {
    if (t1 == t2) {
      return Result.EQUALS;
    }
    if (t1 == null || t2 == null) {
      return Result.UNCOMPARABLE;
    }

    Object d1 = t1.getData();
    Object d2 = t2.getData();
    if (!(d1 instanceof Position && d2 instanceof Position)) {
      return Result.UNCOMPARABLE;
    }

    Position p1 = (Position) d1;
    Position p2 = (Position) d2;
    if (!p1.module.equals(p2.module)) {
      return Result.UNCOMPARABLE;
    }
    if (p1.line < p2.line) {
      return Result.LESS;
    }
    if (p1.line > p2.line) {
      return Result.GREATER;
    }
    if (p1.column < p2.column) {
      return Result.LESS;
    }
    if (p1.column > p2.column) {
      return Result.GREATER;
    }
    return Result.EQUALS;
  }
}
