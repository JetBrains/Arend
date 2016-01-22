package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.SubstVisitor;

import java.util.HashMap;

public class ReferenceExpression extends Expression {
  private final Binding myBinding;

  public ReferenceExpression(Binding binding) {
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
  public Expression getType() {
    return myBinding.getType().accept(new SubstVisitor(new Substitution()), null);
  }
}
