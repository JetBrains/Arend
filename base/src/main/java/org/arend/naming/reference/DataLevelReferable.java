package org.arend.naming.reference;

import org.arend.core.context.binding.ParamLevelVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataLevelReferable implements LevelReferable {
  private final Object myData;
  private final String myName;
  private ParamLevelVariable myVar;

  public DataLevelReferable(Object data, String name) {
    myData = data;
    myName = name;
  }

  @Override
  public @Nullable Object getData() {
    return myData;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myName;
  }

  @Override
  public @NotNull Referable getUnderlyingReferable() {
    return myData instanceof Referable ? (Referable) myData : this;
  }

  @Override
  public ParamLevelVariable getLevelVariable() {
    return myVar;
  }

  @Override
  public void setLevelVariable(ParamLevelVariable levelVariable) {
    myVar = levelVariable;
  }
}
