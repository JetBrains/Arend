package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;

public class FunctionDefinition extends TypedDefinition {
  private final ChildProperty<FunctionDefinition, Expression> myTerm = new ChildProperty<>(this);

  public Expression getTerm() {
    return myTerm.get();
  }

  public Property<Expression> term() {
    return myTerm;
  }
}
