package org.arend.naming.reference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IndexLevelReferable implements LevelReferable {
  private final Object myData;
  private final String myName;
  private final boolean myPLevels;
  private final int myIndex;

  public IndexLevelReferable(Object data, String name, boolean isPLevels, int index) {
    myData = data;
    myName = name;
    myPLevels = isPLevels;
    myIndex = index;
  }

  public int getIndex() {
    return myIndex;
  }

  @Override
  public @Nullable Object getData() {
    return myData;
  }

  @Override
  public @NotNull Referable.RefKind getRefKind() {
    return myPLevels ? RefKind.PLEVEL : RefKind.HLEVEL;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IndexLevelReferable that = (IndexLevelReferable) o;
    return myPLevels == that.myPLevels && myIndex == that.myIndex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(myPLevels, myIndex);
  }
}
