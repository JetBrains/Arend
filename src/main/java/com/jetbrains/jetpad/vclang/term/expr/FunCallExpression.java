package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

public class FunCallExpression extends DefCallExpression {
  public FunCallExpression(FunctionDefinition definition) {
    super(definition);
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    return ExpressionFactory.Apps(this, thisExpr);
  }

  @Override
  public FunctionDefinition getDefinition() {
    return (FunctionDefinition) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunCall(this, params);
  }

  @Override
  public FunCallExpression toFunCall() {
    return this;
  }

  /*
  public ElimTreeNode getElimTree() {
    return getDefinition().getElimTree().subst(new Substitution(new ExprSubstitution(), getPolyParamsSubst()));
  }

  public DependentLink getParameters() {

  } /**/
}
