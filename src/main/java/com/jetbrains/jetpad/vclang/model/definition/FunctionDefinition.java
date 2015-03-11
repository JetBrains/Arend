package com.jetbrains.jetpad.vclang.model.definition;

import jetbrains.jetpad.model.children.ChildProperty;
import com.jetbrains.jetpad.vclang.model.expr.Expression;

public class FunctionDefinition extends TypedDefinition {
  public final ChildProperty<FunctionDefinition, Expression> term = new ChildProperty<>(this);
}
