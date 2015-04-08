package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

import java.util.List;

public class SigmaExpression extends Expression implements Abstract.SigmaExpression {
  private final List<TypeArgument> myArguments;

  public SigmaExpression(List<TypeArgument> arguments) {
    myArguments = arguments;
  }

  @Override
  public List<TypeArgument> getArguments() {
    return myArguments;
  }

  @Override
  public TypeArgument getArgument(int index) {
    return myArguments.get(index);
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return null;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return null;
  }
}
