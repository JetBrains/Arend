package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

import java.util.List;

public class LamExpression extends Expression implements Abstract.LamExpression {
  private final List<Argument> myArguments;
  private final Expression myBody;

  public LamExpression(List<Argument> arguments, Expression body) {
    myArguments = arguments;
    myBody = body;
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
  public Expression getBody() {
    return myBody;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitLam(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }
}
