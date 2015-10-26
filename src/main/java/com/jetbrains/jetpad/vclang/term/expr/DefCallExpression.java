package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;

import java.util.List;

public abstract class DefCallExpression extends Expression implements Abstract.DefCallExpression {
  private final Definition myDefinition;

  public DefCallExpression(Definition definition) {
    myDefinition = definition;
  }

  @Override
  public ResolvedName getResolvedName() {
    return new ResolvedName(myDefinition.getParentNamespace(), myDefinition.getName());
  }

  public Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public Abstract.Expression getExpression() {
    return null;
  }

  @Override
  public Name getName() {
    return myDefinition.getName();
  }

  @Override
  public void setResolvedName(ResolvedName name) {
    throw new IllegalStateException();
  }

  @Override
  public Expression getType(List<Binding> context) {
    return myDefinition.getType();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDefCall(this, params);
  }
}
