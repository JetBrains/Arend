package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class VarExpression extends Expression implements Abstract.DefCallExpression {
  private final Name myName;

  public VarExpression(String name) {
    myName = new Name(name);
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return (T) this;
  }

  @Override
  public Name getName() {
    return myName;
  }

  @Override
  public Abstract.Expression getExpression() {
    return null;
  }

  @Override
  public ResolvedName getResolvedName() {
    return null;
  }

  @Override
  public void setResolvedName(ResolvedName name) {
    throw new IllegalStateException();
  }

  @Override
  public Expression getType(List<Binding> context) {
    return null;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDefCall(this, params);
  }
}
