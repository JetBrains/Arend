package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.Set;

public abstract class DependentTypeExpression extends Expression implements Type {
  private final DependentLink myLink;

  public DependentTypeExpression(DependentLink link) {
    myLink = link;
  }

  public DependentLink getParameters() {
    return myLink;
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public DependentTypeExpression subst(LevelSubstitution substitution) {
    return (DependentTypeExpression) super.subst(substitution);
  }

  @Override
  public DependentTypeExpression strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return (DependentTypeExpression) super.strip(bounds, errorReporter);
  }
}
