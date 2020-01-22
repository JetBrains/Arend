package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.context.CoreEvaluatingBinding;

import javax.annotation.Nonnull;

public interface EvaluatingBinding extends Binding, CoreEvaluatingBinding {
  @Nonnull @Override Expression getExpression();
  void subst(SubstVisitor visitor);
  void subst(InPlaceLevelSubstVisitor visitor);
}
