package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;

public abstract class DependentTypeExpression extends Expression {
  private final LevelArguments myLevelArguments;
  private final DependentLink myLink;

  public DependentTypeExpression(LevelArguments levelArguments, DependentLink link) {
    myLevelArguments = levelArguments;
    myLink = link;
  }

  public LevelArguments getLevelArguments() {
    return myLevelArguments;
  }

  public DependentLink getParameters() {
    return myLink;
  }
}
