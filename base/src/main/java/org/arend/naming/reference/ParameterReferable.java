package org.arend.naming.reference;

import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ParameterReferable implements TypedReferable {
  private final TCDefReferable myDefinition;
  private final int myIndex;
  private final Referable myReferable;
  private final Concrete.ReferenceExpression myClassRef;

  public ParameterReferable(TCDefReferable definition, int index, Referable referable, Concrete.ReferenceExpression classRef) {
    myDefinition = definition;
    myIndex = index;
    myReferable = referable;
    myClassRef = classRef;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myReferable.textRepresentation();
  }

  public TCDefReferable getDefinition() {
    return myDefinition;
  }

  public int getIndex() {
    return myIndex;
  }

  @Override
  public @NotNull Referable getUnderlyingReferable() {
    return myReferable;
  }

  @Override
  public @Nullable ClassReferable getTypeClassReference() {
    Referable ref = myClassRef == null ? null : myClassRef.getReferent();
    return ref instanceof ClassReferable ? (ClassReferable) ref : null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParameterReferable that = (ParameterReferable) o;
    return myIndex == that.myIndex && myDefinition.equals(that.myDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myDefinition, myIndex);
  }
}
