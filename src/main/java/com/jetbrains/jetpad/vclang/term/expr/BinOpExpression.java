package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class BinOpExpression extends Expression implements Abstract.BinOpExpression {
  private final ArgumentExpression myLeft;
  private final ArgumentExpression myRight;
  private final Definition myBinOp;

  public BinOpExpression(ArgumentExpression left, Definition binOp, ArgumentExpression right) {
    myLeft = left;
    myRight = right;
    myBinOp = binOp;
  }

  @Override
  public ArgumentExpression getLeft() {
    return myLeft;
  }

  @Override
  public ArgumentExpression getRight() {
    return myRight;
  }

  @Override
  public Definition getBinOp() {
    return myBinOp;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitBinOp(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitBinOp(this, params);
  }
}
