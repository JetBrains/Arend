package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;

  public DefCallExpression(Definition definition) {
    myDefinition = definition;
  }


  public abstract Expression applyThis(Expression thisExpr);

  public Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public Expression getType() {
    return myDefinition.getType();
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
