package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public Expression getType(List<Binding> context) {
    List<Expression> arguments = new ArrayList<>();
    Expression function = getFunction(arguments);
    Expression type = function.getType(context);

    Map<Binding, Expression> substs = new HashMap<>();
    for (int i = arguments.size() - 1; i >=0; i--) {
      if (type instanceof DependentExpression && type instanceof DependentExpression.Pi) {
        substs.put((DependentExpression) type, arguments.get(i));
      } else {
        return null;
      }
    }

    return type.subst(substs);
  }
}
