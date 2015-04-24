package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class BinOpExpression extends Expression implements Abstract.BinOpExpression {
  private final Expression myLeft;
  private final Expression myRight;
  private final Definition myBinOp;

  public BinOpExpression(Expression left, Definition binOp, Expression right) {
    myLeft = left;
    myRight = right;
    myBinOp = binOp;
  }

  @Override
  public Expression getLeft() {
    return myLeft;
  }

  @Override
  public Expression getRight() {
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
