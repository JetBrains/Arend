package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public abstract class TypedDefinition extends Definition {
  public final Property<String> name = new ValueProperty<>();
  public final ObservableList<Argument> arguments = new ObservableArrayList<>();
  public final Property<Expression> resultType = new ValueProperty<>();
}
