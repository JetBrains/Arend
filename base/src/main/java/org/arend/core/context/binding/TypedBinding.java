package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.subst.InPlaceLevelSubstVisitor;

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

  @Override
  public void subst(InPlaceLevelSubstVisitor substVisitor) {
    myType.accept(substVisitor, null);
  }

  public String toString() {
    return getName();
  }
}
