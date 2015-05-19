package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class TupleExpression extends Expression implements Abstract.TupleExpression {
  private final List<Expression> myFields;

  public TupleExpression(List<Expression> fields) {
    myFields = fields;
  }

  @Override
  public List<Expression> getFields() {
    return myFields;
  }

  @Override
  public Expression getField(int index) {
    return myFields.get(index);
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitTuple(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTuple(this, params);
  }
}
