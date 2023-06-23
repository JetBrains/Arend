package org.arend.naming.reference;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class InferenceLevelReferable implements LevelReferable {
  private final Object myData;
  private final InferenceLevelVariable myVariable;

  public InferenceLevelReferable(Object data, InferenceLevelVariable variable) {
    myData = data;
    myVariable = variable;
  }

  public InferenceLevelVariable getVariable() {
    return myVariable;
  }

  @Override
  public @Nullable Object getData() {
    return myData;
  }

  @Override
  public @NotNull Referable.RefKind getRefKind() {
    return myVariable.getType() == LevelVariable.LvlType.PLVL ? RefKind.PLEVEL : RefKind.HLEVEL;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myVariable.getName();
  }

  @Override
  public boolean isInferenceRef() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InferenceLevelReferable that = (InferenceLevelReferable) o;
    return Objects.equals(myVariable, that.myVariable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myVariable);
  }

  @Override
  public String toString() {
    return textRepresentation();
  }
}
