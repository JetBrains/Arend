package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.ext.core.context.CoreBinding;

public interface Binding extends CoreBinding {
  @Override Expression getTypeExpr();
  void strip(StripVisitor stripVisitor);
  void subst(InPlaceLevelSubstVisitor substVisitor);

  default boolean isHidden() {
    return false;
  }

  default Type getType() {
    Expression type = getTypeExpr();
    if (type instanceof Type) return (Type) type;
    Sort sort = type.getSortOfType();
    return sort == null ? null : new TypeExpression(type, sort);
  }

  @Override
  default ReferenceExpression makeReference() {
    return new ReferenceExpression(this);
  }
}
