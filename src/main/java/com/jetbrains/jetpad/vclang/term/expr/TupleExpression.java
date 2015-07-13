package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class TupleExpression extends Expression implements Abstract.TupleExpression {
  private final List<Expression> myFields;
  private final SigmaExpression myType;

  public TupleExpression(List<Expression> fields, SigmaExpression type) {
    myFields = fields;
    myType = type;
  }

  @Override
  public List<Expression> getFields() {
    return myFields;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitTuple(this);
  }

  @Override
  public SigmaExpression getType(List<Expression> context) {
    return myType;
  }

  public SigmaExpression getType() {
    return myType;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTuple(this, params);
  }
}
