package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.StripVisitor;

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

  @Override
  public void strip(StripVisitor stripVisitor) {
    myType = myType.accept(stripVisitor, null);
  }

  public String toString() {
    return getName();
  }
}
