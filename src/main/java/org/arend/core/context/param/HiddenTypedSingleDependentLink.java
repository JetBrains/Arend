package org.arend.core.context.param;

import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.subst.SubstVisitor;

public class HiddenTypedSingleDependentLink extends TypedSingleDependentLink {
  public HiddenTypedSingleDependentLink(boolean isExplicit, String name, Type type) {
    super(isExplicit, name, type);
  }

  @Override
  public SingleDependentLink subst(SubstVisitor substVisitor, int size, boolean updateSubst) {
    if (size > 0) {
      HiddenTypedSingleDependentLink result = new HiddenTypedSingleDependentLink(isExplicit(), getName(), getType().subst(substVisitor));
      if (updateSubst) {
        substVisitor.getExprSubstitution().addSubst(this, new ReferenceExpression(result));
      } else {
        substVisitor.getExprSubstitution().add(this, new ReferenceExpression(result));
      }
      return result;
    } else {
      return EmptyDependentLink.getInstance();
    }
  }
}
