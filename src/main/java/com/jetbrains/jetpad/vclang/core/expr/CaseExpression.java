package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;

import java.util.List;

public class CaseExpression extends Expression {
  private final Expression myResultType;
  private final BranchElimTreeNode myElimTree;
  private final List<Expression> myArguments;

  public CaseExpression(Expression resultType, BranchElimTreeNode elimTree, List<Expression> arguments) {
    myElimTree = elimTree;
    myResultType = resultType;
    myArguments = arguments;
  }

  public Expression getResultType() {
    return myResultType;
  }

  public BranchElimTreeNode getElimTree() {
    return myElimTree;
  }

  public List<Expression> getArguments() {
    return myArguments;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitCase(this, params);
  }

  @Override
  public CaseExpression toCase() {
    return this;
  }
}
