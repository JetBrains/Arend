package com.jetbrains.jetpad.model.definition;

import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;
import com.jetbrains.jetpad.model.Node;
import com.jetbrains.jetpad.model.expr.Expression;

public class Argument extends Node {
    public final Property<Boolean> explicit = new ValueProperty<Boolean>(true);
    public final Property<String> name = new ValueProperty<String>();
    public final Property<Expression> type = new ValueProperty<Expression>();
}
