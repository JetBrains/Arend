package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class TupleExpression extends Expression {
  private final List<Expression> myFields;
  private final SigmaExpression myType;

  public TupleExpression(List<Expression> fields, SigmaExpression type) {
    myFields = fields;
    myType = type;
  }

  public List<Expression> getFields() {
    return myFields;
  }

  @Override
  public SigmaExpression getType() {
    return myType;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTuple(this, params);
  }

  @Override
  public TupleExpression toTuple() {
    return this;
  }
}
