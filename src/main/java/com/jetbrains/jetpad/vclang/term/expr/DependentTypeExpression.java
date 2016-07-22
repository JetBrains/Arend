package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;

public abstract class DependentTypeExpression extends Expression {
  private final DependentLink myLink;

  public DependentTypeExpression(DependentLink link) {
    myLink = link;
  }

  @Override
  public DependentLink getParameters() {
    return myLink;
  }
}
