package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelArguments;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;
  private LevelArguments myPolyArguments;

  public DefCallExpression(Definition definition, LevelArguments polyParams) {
    myDefinition = definition;
    myPolyArguments = polyParams;
  }

  public List<? extends Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public LevelArguments getPolyArguments() {
    return myPolyArguments;
  }

  public abstract Expression applyThis(Expression thisExpr);

  public Definition getDefinition() {
    return myDefinition;
  }

  public void setPolyParamsSubst(LevelArguments polyParams) {
    myPolyArguments = polyParams;
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
