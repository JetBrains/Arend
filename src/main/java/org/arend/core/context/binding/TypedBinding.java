package org.arend.core.context.binding;

import org.arend.core.expr.Expression;

public class TypedBinding extends NamedBinding {
  private Expression myType;

  public TypedBinding(String name, Expression type) {
    super(name);
    myType = type;
  }

  @Override
  public Expression getTypeExpr() {
    return myType;
  }

  public void setTypeExpr(Expression type) {
    myType = type;
  }

  public String toString() {
    return getName();
  }
}
