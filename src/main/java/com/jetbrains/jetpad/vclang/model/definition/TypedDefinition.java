package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.model.children.ChildList;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public abstract class TypedDefinition extends Definition {
  private final Property<String> myName = new ValueProperty<>();
  private final ChildList<TypedDefinition, Argument> myArguments = new ChildList<>(this);
  private final ChildProperty<TypedDefinition, Expression> myResultType = new ChildProperty<>(this);

  public String getName() {
    return myName.get();
  }

  public ObservableList<Argument> arguments() {
    return myArguments;
  }

  public Expression getResultType() {
    return myResultType.get();
  }

  public Property<String> name() {
    return myName;
  }

  public Property<Expression> resultType() {
    return myResultType;
  }
}
