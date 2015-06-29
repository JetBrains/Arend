package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.Collection;
import java.util.Map;

public class ClassExtExpression extends Expression implements Abstract.ClassExtExpression {
  private final ClassDefinition myBaseClass;
  private final Map<FunctionDefinition, FunctionDefinition> myDefinitions;

  public ClassExtExpression(ClassDefinition baseClass, Map<FunctionDefinition, FunctionDefinition> definitions) {
    myBaseClass = baseClass;
    myDefinitions = definitions;
  }

  @Override
  public ClassDefinition getBaseClass() {
    return myBaseClass;
  }

  @Override
  public Collection<FunctionDefinition> getDefinitions() {
    return myDefinitions.values();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitClassExt(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassExt(this, params);
  }
}
