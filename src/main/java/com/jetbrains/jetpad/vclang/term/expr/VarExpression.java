package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class VarExpression extends Expression implements Abstract.DefCallExpression {
  private final Utils.Name myName;

  public VarExpression(String name) {
    myName = new Utils.Name(name);
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return (T) this;
  }

  @Override
  public Utils.Name getName() {
    return myName;
  }

  @Override
  public Abstract.Expression getExpression() {
    return null;
  }

  @Override
  public DefinitionPair getDefinitionPair() {
    return null;
  }

  @Override
  public void replaceWithDefCall(DefinitionPair definition) {
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
