package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;
  private LevelArguments myLevelArguments;

  public DefCallExpression(Definition definition, LevelArguments polyArgs) {
    myDefinition = definition;
    myLevelArguments = polyArgs;
  }

  public List<? extends Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public LevelArguments getLevelArguments() {
    return myLevelArguments;
  }

  public Definition getDefinition() {
    return myDefinition;
  }

  public void setLevelArguments(LevelArguments polyParams) {
    myLevelArguments = polyParams;
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
