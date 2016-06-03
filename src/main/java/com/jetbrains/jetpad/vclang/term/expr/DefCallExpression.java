package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;
  private LevelSubstitution myPolyParamsSubst;

  public DefCallExpression(Definition definition) {
    myDefinition = definition;
    myPolyParamsSubst = new LevelSubstitution();
  }

  public DefCallExpression(Definition definition, LevelSubstitution subst) {
    myDefinition = definition;
    myPolyParamsSubst = subst;
  }

  public abstract Expression applyThis(Expression thisExpr);

  public Definition getDefinition() {
    return myDefinition;
  }

  public LevelSubstitution getPolyParamsSubst() {
    return myPolyParamsSubst;
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
