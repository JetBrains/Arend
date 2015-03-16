package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

// TODO: Replace myName with a list of variables
public class Argument extends Expression {
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

  public Property<Boolean> isExplicit() {
    return myExplicit;
  }

  public Property<String> name() {
    return myName;
  }

  public Property<Expression> type() {
    return myType;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    throw new IllegalStateException();
  }
}
