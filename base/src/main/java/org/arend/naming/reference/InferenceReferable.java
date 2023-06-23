package org.arend.naming.reference;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InferenceReferable implements Referable {
  private final InferenceVariable myVar;

  public InferenceReferable(InferenceVariable var) {
    myVar = var;
  }

  public InferenceVariable getVariable() {
    return myVar;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myVar.getName();
  }

  @Override
  public boolean isInferenceRef() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InferenceReferable that = (InferenceReferable) o;
    return Objects.equals(myVar, that.myVar);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myVar);
  }
}
