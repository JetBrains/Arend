package com.jetbrains.jetpad.vclang.model.definition;

import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;

import static com.jetbrains.jetpad.vclang.model.expr.Model.Expression;

public class FunctionDefinition extends TypedDefinition {
  private final ChildProperty<FunctionDefinition, Expression> myTerm = new ChildProperty<>(this);

  public Expression getTerm() {
    return myTerm.get();
  }

  public Property<Expression> term() {
    return myTerm;
  }
}
