package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;

public class PiExpression extends Expression implements Abstract.PiExpression {
  private final ChildProperty<PiExpression, Expression> myDomain = new ChildProperty<>(this);
  private final ChildProperty<PiExpression, Expression> myCodomain = new ChildProperty<>(this);

  @Override
  public boolean isExplicit() {
    if (myDomain.get() instanceof Argument) {
      return ((Argument) myDomain.get()).getExplicit();
    } else {
      return true;
    }
  }

  @Override
  public String getVariable() {
    if (myDomain.get() instanceof Argument) {
      return ((Argument) myDomain.get()).getName();
    } else {
      return null;
    }
  }

  @Override
  public Expression getDomain() {
    if (myDomain.get() instanceof Argument) {
      return ((Argument) myDomain.get()).getType();
    } else {
      return myDomain.get();
    }
  }

  @Override
  public Expression getCodomain() {
    return myCodomain.get();
  }

  public Property<Expression> domain() {
    return myDomain;
  }

  public Property<Expression> codomain() {
    return myCodomain;
  }
  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }
}
