package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class ElimExpression extends Expression implements Abstract.ElimExpression {
  // TODO: separate elim and case expressions.
  private final IndexExpression myExpression;
  private final List<Clause> myClauses;
  private final Clause myOtherwise;

  public ElimExpression(IndexExpression expression, List<Clause> clauses, Clause otherwise) {
    myExpression = expression;
    myClauses = clauses;
    myOtherwise = otherwise;
  }

  public ElimType getElimType() {
    return ElimType.ELIM;
  }

  @Override
  public IndexExpression getExpression() {
    return myExpression;
  }

  @Override
  public List<Clause> getClauses() {
    return myClauses;
  }

  @Override
  public Clause getOtherwise() {
    return myOtherwise;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitElim(this);
  }

  @Override
  public Expression getType(List<Binding> context) {
    return null;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitElim(this, params);
  }
}
