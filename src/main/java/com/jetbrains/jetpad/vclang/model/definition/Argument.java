package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.Position;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.DelegateProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class Argument extends Node {
  private final Property<Boolean> myExplicit = new ValueProperty<>(true);
  private final Property<String> myName = new ValueProperty<>();
  private final ChildProperty<Argument, Expression> myType = new ChildProperty<>(this);

  public Boolean getExplicit() {
    return myExplicit.get();
  }

  public String getName() {
    return myName.get();
  }

  public Expression getType() {
    return myType.get();
  }

  public Property<String> name() {
    return myName;
  }

  public Property<Expression> type() {
    return new DelegateProperty<Expression>(myType) {
      @Override
      public void set(Expression type) {
        Argument.this.setType(type);
      }
    };
  }

  public void setExplicit(Boolean explicit) {
    Expression type = myType.get();
    if (type != null) {
      if (myName.get() == null && explicit) {
        type.position = position;
      } else {
        type.position = Position.ARG;
      }
    }
    myExplicit.set(explicit);
  }

  public void setName(String name) {
    Expression type = myType.get();
    if (type != null) {
      if (name == null && myExplicit.get()) {
        type.position = position;
      } else {
        type.position = Position.ARG;
      }
    }
    myName.set(name);
  }

  public void setType(Expression type) {
    if (type != null) {
      if (myName.get() == null && myExplicit.get()) {
        type.position = position;
      } else {
        type.position = Position.ARG;
      }
    }
    myType.set(type);
  }
}
