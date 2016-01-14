package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.HashMap;
import java.util.Map;

public class ProjExpression extends Expression {
  private final Expression myExpression;
  private final int myField;

  public ProjExpression(Expression expression, int field) {
    myExpression = expression;
    myField = field;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public int getField() {
    return myField;
  }

  @Override
  public Expression getType() {
    Expression type = myExpression.getType();
    if (!(type instanceof SigmaExpression)) return null;
    DependentLink params = ((SigmaExpression) type).getLink();
    if (myField == 0) {
      return params.getType();
    }

    Map<Binding, Expression> substs = new HashMap<>();
    for (int i = 0; i < myField; i++) {
      if (params == null) {
        return null;
      }
      substs.put(params, new ProjExpression(myExpression, i));
      params = params.getNext();
    }
    return params.getType().subst(substs);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitProj(this, params);
  }
}
