package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;

import java.util.List;

public class Clause implements Abstract.Clause {
  private final Constructor myConstructor;
  private final List<Argument> myArguments;
  private final Abstract.Definition.Arrow myArrow;
  private final Expression myExpression;

  public Clause(Constructor constructor, List<Argument> arguments, Abstract.Definition.Arrow arrow, Expression expression) {
    myConstructor = constructor;
    myArguments = arguments;
    myArrow = arrow;
    myExpression = expression;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  @Override
  public String getName() {
    return myConstructor.getName();
  }

  @Override
  public List<Argument> getArguments() {
    return myArguments;
  }

  @Override
  public Argument getArgument(int index) {
    return myArguments.get(index);
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return myArrow;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }
}
