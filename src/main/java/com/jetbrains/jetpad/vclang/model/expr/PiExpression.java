package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Position;
import com.jetbrains.jetpad.vclang.model.definition.Argument;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.DelegateProperty;
import jetbrains.jetpad.model.property.Property;

public class PiExpression extends Expression implements Abstract.PiExpression {
  private final ChildProperty<PiExpression, Argument> myDomain = new ChildProperty<>(this);
  private final ChildProperty<PiExpression, Expression> myCodomain = new ChildProperty<>(this);

  @Override
  public boolean isExplicit() {
    return myDomain.get().getExplicit();
  }

  @Override
  public String getVariable() {
    return myDomain.get().getName();
  }

  @Override
  public Expression getDomain() {
    return myDomain.get().getType();
  }

  @Override
  public Expression getCodomain() {
    return myCodomain.get();
  }

  public Property<Argument> domain() {
    return new DelegateProperty<Argument>(myDomain) {
      @Override
      public void set(Argument domain) {
        PiExpression.this.setDomain(domain);
      }
    };
  }

  public Property<Expression> codomain() {
    return new DelegateProperty<Expression>(myCodomain) {
      @Override
      public void set(Expression codomain) {
        PiExpression.this.setCodomain(codomain);
      }
    };
  }

  public void setDomain(Argument domain) {
    if (domain != null) {
      domain.position = Position.ARR_DOM;
    }
    myDomain.set(domain);
  }

  public void setCodomain(Expression codomain) {
    if (codomain != null) {
      codomain.position = Position.ARR_COD;
    }
    myCodomain.set(codomain);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }
}
