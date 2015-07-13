package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class ProjExpression extends Expression implements Abstract.ProjExpression {
  private final Expression myExpression;
  private final int myField;

  public ProjExpression(Expression expression, int field) {
    myExpression = expression;
    myField = field;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public int getField() {
    return myField;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitProj(this);
  }

  @Override
  public Expression getType(List<Expression> context) {
    Expression type = myExpression.getType(context);
    if (!(type instanceof SigmaExpression)) return null;
    List<TypeArgument> arguments = ((SigmaExpression) type).getArguments();
    int index = 0;
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        index += ((TelescopeArgument) argument).getNames().size();
        if (myField < index) {
          return argument.getType();
        }
      } else {
        if (myField == index) {
          return argument.getType();
        }
        ++index;
      }
    }
    return null;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitProj(this, params);
  }
}
