package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class VarExpression extends Expression {
  private final Name myName;

  public VarExpression(String name) {
    myName = new Name(name);
  }

  public Name getName() {
    return myName;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return (R) this;
  }

  @Override
  public Expression getType(List<Binding> context) {
    return null;
  }
}
