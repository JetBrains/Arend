package com.jetbrains.jetpad.vclang.model.definition;

import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;
import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Expression;

public class Argument extends Node {
  public final Property<Boolean> explicit = new ValueProperty<>(true);
  public final Property<String> name = new ValueProperty<>();
  public final ChildProperty<Argument, Expression> type = new ChildProperty<>(this);
}
