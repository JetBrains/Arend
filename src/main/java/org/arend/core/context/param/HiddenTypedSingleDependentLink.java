package org.arend.core.context.param;

import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;

public class HiddenTypedSingleDependentLink extends TypedSingleDependentLink {
  public HiddenTypedSingleDependentLink(boolean isExplicit, String name, Type type) {
    super(isExplicit, name, type);
  }

  @Override
  public SingleDependentLink subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, int size, boolean updateSubst) {
    if (size > 0) {
      HiddenTypedSingleDependentLink result = new HiddenTypedSingleDependentLink(isExplicit(), getName(), getType().subst(exprSubst, levelSubst));
      if (updateSubst) {
        exprSubst.addSubst(this, new ReferenceExpression(result));
      } else {
        exprSubst.add(this, new ReferenceExpression(result));
      }
      return result;
    } else {
      return EmptyDependentLink.getInstance();
    }
  }
}
