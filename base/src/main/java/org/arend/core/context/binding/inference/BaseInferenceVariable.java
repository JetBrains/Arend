package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.Variable;
import org.arend.core.expr.Expression;
import org.arend.ext.core.context.CoreInferenceVariable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class BaseInferenceVariable implements Variable, CoreInferenceVariable {
  private final String myName;
  private Expression myType;
  private final Set<Binding> myBounds;

  public BaseInferenceVariable(String name, Expression type, Set<Binding> bounds) {
    myName = name == null || name.isEmpty() ? "x" : name;
    myType = type;
    myBounds = bounds;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  public Expression getType() {
    return myType;
  }

  public void setType(Expression type) {
    myType = type;
  }

  public Set<Binding> getBounds() {
    return myBounds;
  }

  @Override
  public String toString() {
    return "?" + getName();
  }
}
