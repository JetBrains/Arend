package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
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
  public Expression getType() {
    List<Expression> arguments = new ArrayList<>();
    Expression function = getFunction(arguments);
    Expression type = function.getType();

    Map<Binding, Expression> substs = new HashMap<>();
    if (!(type instanceof PiExpression)) {
      return null;
    }
    DependentLink link = ((PiExpression) type).getLink();
    for (int i = arguments.size() - 1; i >= 0; i--, link = link.getNext()) {
      assert link != null;
      substs.put(link, arguments.get(i));
    }

    type = type.subst(substs);
    return link == null ? type : new PiExpression(link.subst(substs), type);
  }
}
