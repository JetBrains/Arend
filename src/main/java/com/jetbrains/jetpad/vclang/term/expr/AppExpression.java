package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AppExpression extends Expression {
  private Expression myFunction;
  private List<Expression> myArguments;

  private void initialize(Expression function, Collection<? extends Expression> arguments) {
    assert !arguments.isEmpty();

    myFunction = function.getFunction();
    AppExpression app = function.toApp();
    if (app != null) {
      myArguments = new ArrayList<>(app.getArguments().size() + arguments.size());
      myArguments.addAll(app.getArguments());
      myArguments.addAll(arguments);
    }
  }

  public AppExpression(Expression function, Collection<? extends Expression> arguments) {
    initialize(function, arguments);
    if (myArguments == null) {
      myArguments = new ArrayList<>(arguments);
    }
  }

  public AppExpression(Expression function, List<Expression> arguments) {
    initialize(function, arguments);
    if (myArguments == null) {
      myArguments = arguments;
    }
  }

  @Override
  public Expression getFunction() {
    return myFunction;
  }

  @Override
  public List<? extends Expression> getArguments() {
    return myArguments;
  }

  @Override
  public AppExpression addArgument(Expression argument) {
    myArguments.add(argument);
    return this;
  }

  @Override
  public AppExpression addArguments(Collection<? extends Expression> arguments) {
    myArguments.addAll(arguments);
    return this;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }

  @Override
  public AppExpression toApp() {
    return this;
  }
}
