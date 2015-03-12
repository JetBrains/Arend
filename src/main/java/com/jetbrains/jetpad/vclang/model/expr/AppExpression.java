package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;

public class AppExpression extends Expression implements Abstract.AppExpression {
  private final ChildProperty<AppExpression, Expression> myFunction = new ChildProperty<>(this);
  private final ChildProperty<AppExpression, Expression> myArgument = new ChildProperty<>(this);

  @Override
  public Expression getFunction() {
    return myFunction.get();
  }

  @Override
  public Expression getArgument() {
    return myArgument.get();
  }

  public Property<Expression> function() {
    return myFunction;
  }

  public Property<Expression> argument() {
    return myArgument;
  }
  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }
}
