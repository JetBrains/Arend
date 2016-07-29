package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class ReferenceExpression extends Expression {
  private final Binding myBinding;

  public ReferenceExpression(Binding binding) {
    assert !(binding instanceof Definition);
    assert binding != EmptyDependentLink.getInstance();
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
  public ReferenceExpression toReference() {
    return this;
  }
}
