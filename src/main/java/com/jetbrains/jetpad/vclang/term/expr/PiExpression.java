package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

public class PiExpression extends Expression implements Abstract.PiExpression {
  private final List<TypeArgument> myArguments;
  private final Expression myCodomain;

  public PiExpression(Expression domain, Expression codomain) {
    this(new ArrayList<TypeArgument>(1), codomain.liftIndex(0, 1));
    myArguments.add(new TypeArgument(true, domain));
  }

  public PiExpression(List<TypeArgument> arguments, Expression codomain) {
    myArguments = arguments;
    myCodomain = codomain;
  }

  @Override
  public List<TypeArgument> getArguments() {
    return myArguments;
  }

  @Override
  public TypeArgument getArgument(int index) {
    return myArguments.get(index);
  }

  @Override
  public Expression getCodomain() {
    return myCodomain;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitPi(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }
}
