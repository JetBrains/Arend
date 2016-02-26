package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppExpression extends Expression {
  private final Expression myFunction;
  private final ArgumentExpression myArgument;

  public AppExpression(Expression function, ArgumentExpression argument) {
    myFunction = function;
    myArgument = argument;
  }

  public Expression getFunction() {
    return myFunction;
  }

  public ArgumentExpression getArgument() {
    return myArgument;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }

  @Override
  public Expression getType() {
    List<Expression> arguments = new ArrayList<>();
    Expression functionType = getFunction(arguments).getType();
    if (functionType != null) {
      Collections.reverse(arguments);
      return functionType.applyExpressions(arguments);
    } else {
      return null;
    }
  }
}
