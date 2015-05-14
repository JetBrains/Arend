package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintClause;

public class Clause implements Abstract.Clause {
  private final Constructor myConstructor;
  private final List<Argument> myArguments;
  private final Abstract.Definition.Arrow myArrow;
  private final Expression myExpression;
  private ElimExpression myElimExpression;

  public Clause(Constructor constructor, List<Argument> arguments, Abstract.Definition.Arrow arrow, Expression expression, ElimExpression elimExpression) {
    myConstructor = constructor;
    myArguments = arguments;
    myArrow = arrow;
    myExpression = expression;
    myElimExpression = elimExpression;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  public void setElimExpression(ElimExpression elimExpression) {
    myElimExpression = elimExpression;
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

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintClause(myElimExpression, this, builder, names, 0);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
    return builder.toString();
  }
}
