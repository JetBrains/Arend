package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class AppExpression extends Expression {
  public final static int PREC = 10;

  private final Expression function;
  private final Expression argument;

  public AppExpression(Expression function, Expression argument) {
    this.function = function;
    this.argument = argument;
  }

  public Expression getFunction() {
    return function;
  }

  public Expression getArgument() {
    return argument;
  }

  @Override
  public void prettyPrint(PrintStream stream, List<String> names, int prec) {
    if (prec > PREC) stream.print("(");
    function.prettyPrint(stream, names, PREC);
    stream.print(" ");
    argument.prettyPrint(stream, names, PREC + 1);
    if (prec > PREC) stream.print(")");
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof AppExpression)) return false;
    AppExpression other = (AppExpression)o;
    return function.equals(other.function) && argument.equals(other.argument);
  }

  @Override
  public String toString() {
    return "(" + function.toString() + ") (" + argument.toString() + ")";
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitApp(this);
  }
}
