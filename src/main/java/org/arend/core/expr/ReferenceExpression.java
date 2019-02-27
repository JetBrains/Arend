package org.arend.core.expr;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.visitor.ExpressionVisitor;

public class ReferenceExpression extends Expression {
  private final Binding myBinding;

  public ReferenceExpression(Binding binding) {
    assert !(binding instanceof Definition);
    assert !(binding instanceof InferenceVariable);
    assert binding != EmptyDependentLink.getInstance();
    assert binding != null;
    myBinding = binding;
  }

  public Binding getBinding() {
    return myBinding;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitReference(this, params);
  }

  @Override
  public boolean isWHNF() {
    return !(myBinding instanceof LetClause || myBinding instanceof EvaluatingBinding);
  }

  @Override
  public Expression getStuckExpression() {
    return myBinding instanceof LetClause || myBinding instanceof EvaluatingBinding ? null : this;
  }
}
