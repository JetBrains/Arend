package org.arend.core.context.binding;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.ext.core.ops.CMP;

public class ParamLevelVariable implements LevelVariable {
  private final LvlType myLevelType;
  private final String myName;
  private final int mySize;

  public ParamLevelVariable(LvlType levelType, String name, int size) {
    myLevelType = levelType;
    myName = name;
    mySize = size;
  }

  @Override
  public LvlType getType() {
    return myLevelType;
  }

  @Override
  public LevelVariable max(LevelVariable other) {
    return other instanceof InferenceLevelVariable || getType() != other.getType() ? null : !(other instanceof ParamLevelVariable) || mySize >= ((ParamLevelVariable) other).mySize ? this : other;
  }

  private static boolean compare(int n1, int n2, CMP cmp) {
    return cmp == CMP.LE ? n1 <= n2 : cmp == CMP.GE ? n1 >= n2 : n1 == n2;
  }

  @Override
  public boolean compare(LevelVariable other, CMP cmp) {
    return other == getStd() && (mySize == 0 || cmp == CMP.GE) || other instanceof ParamLevelVariable && compare(mySize, ((ParamLevelVariable) other).mySize, cmp);
  }

  public int getSize() {
    return mySize;
  }

  @Override
  public String toString() {
    return myName;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof LevelVariable && mySize == 0 && o == getStd();
  }

  @Override
  public int hashCode() {
    return mySize == 0 ? getStd().hashCode() : super.hashCode();
  }
}
