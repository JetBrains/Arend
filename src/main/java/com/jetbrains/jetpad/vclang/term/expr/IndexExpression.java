package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class IndexExpression extends Expression implements Abstract.IndexExpression {
  private final int myIndex;

  public IndexExpression(int index) {
    myIndex = index;
  }

  @Override
  public int getIndex() {
    return myIndex;
  }

  @Override
  public Expression getType(List<Binding> context) {
    return context.get(context.size() - 1 - myIndex).lift(myIndex + 1).getType();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitIndex(this, params);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitIndex(this, params);
  }
}
