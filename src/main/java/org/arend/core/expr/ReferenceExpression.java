package org.arend.core.expr;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreReferenceExpression;
import org.arend.util.Decision;

import javax.annotation.Nonnull;

public class ReferenceExpression extends Expression implements CoreReferenceExpression {
  private final Binding myBinding;

  public ReferenceExpression(Binding binding) {
    assert !(binding instanceof Definition);
    assert !(binding instanceof InferenceVariable);
    assert binding != EmptyDependentLink.getInstance();
    assert binding != null;
    myBinding = binding;
  }

  @Nonnull
  @Override
  public Binding getBinding() {
    return myBinding;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitReference(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitReference(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@Nonnull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitReference(this, params);
  }

  @Override
  public Decision isWHNF() {
    return myBinding instanceof EvaluatingBinding ? Decision.NO : Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return myBinding instanceof EvaluatingBinding ? null : this;
  }
}
