package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.subst.SubstVisitor;

public interface EvaluatingBinding extends Binding {
  Expression getExpression();
  void subst(SubstVisitor visitor);
}
