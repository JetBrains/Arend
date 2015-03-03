package com.jetbrains.jetpad.vclang.model.definition;

import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;
import com.jetbrains.jetpad.vclang.model.expr.Expression;

public class FunctionDefinition extends TypedDefinition {
  public final Property<Expression> term = new ValueProperty<>();
}
