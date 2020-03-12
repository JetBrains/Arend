package org.arend.typechecking.order;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PartialComparator<T> {
  enum Result { LESS, EQUALS, GREATER, UNCOMPARABLE }

  @NotNull Result compare(@Nullable T t1, @Nullable T t2);

  default void sort(List<T> list) {
    for (int i = 0; i < list.size(); i++) {
      int minIndex = i;
      for (int j = i + 1; j < list.size(); j++) {
        if (compare(list.get(j), list.get(minIndex)) == Result.LESS) {
          minIndex = j;
        }
      }
      if (minIndex != i) {
        T t = list.get(i);
        list.set(i, list.get(minIndex));
        list.set(minIndex, t);
      }
    }
  }
}
