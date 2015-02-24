package com.jetbrains.jetpad.model.definition;

import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;
import com.jetbrains.jetpad.model.expr.Expression;

public class FunctionDefinition extends Definition {
    public final Property<Expression> term = new ValueProperty<Expression>();
}
