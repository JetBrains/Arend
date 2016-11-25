package com.jetbrains.jetpad.vclang.parser.prettyprint;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class AbstractExpressionPP implements PrettyPrintable {
  private final Abstract.Expression myExpression;

  public AbstractExpressionPP(Abstract.Expression expression) {
    myExpression = expression;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    myExpression.accept(new PrettyPrintVisitor(builder, indent), prec);
  }
}
