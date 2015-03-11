package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Position;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.DelegateProperty;
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
    return new DelegateProperty<Expression>(myFunction) {
      @Override
      public void set(Expression function) {
        AppExpression.this.setFunction(function);
      }
    };
  }

  public Property<Expression> argument() {
    return new DelegateProperty<Expression>(myArgument) {
      @Override
      public void set(Expression argument) {
        AppExpression.this.setArgument(argument);
      }
    };
  }

  public void setFunction(Expression function) {
    myFunction.set(function);
    if (function != null) {
      function.position = Position.APP_FUN;
    }
  }

  public void setArgument(Expression argument) {
    myArgument.set(argument);
    if (argument != null) {
      argument.position = Position.APP_ARG;
    }
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }
}
