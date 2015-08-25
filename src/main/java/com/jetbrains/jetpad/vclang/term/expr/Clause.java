package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintClause;

public class Clause implements Abstract.Clause {
  private final Pattern myPattern;
  private final Abstract.Definition.Arrow myArrow;
  private final Expression myExpression;
  private ElimExpression myElimExpression;

  public Clause(Pattern pattern, Abstract.Definition.Arrow arrow, Expression expression, ElimExpression elimExpression) {
    myPattern = pattern;
    myArrow = arrow;
    myExpression = expression;
    myElimExpression = elimExpression;
  }


  public void setElimExpression(ElimExpression elimExpression) {
    myElimExpression = elimExpression;
  }

  @Override
  public Pattern getPattern() {
    return myPattern;
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
