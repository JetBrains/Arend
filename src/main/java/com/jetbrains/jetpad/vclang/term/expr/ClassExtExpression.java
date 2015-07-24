package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ClassExtExpression extends Expression implements Abstract.ClassExtExpression {
  private final ClassDefinition myBaseClass;
  private final Map<FunctionDefinition, OverriddenDefinition> myDefinitions;
  private final Universe myUniverse;

  public ClassExtExpression(ClassDefinition baseClass, Map<FunctionDefinition, OverriddenDefinition> definitions, Universe universe) {
    myBaseClass = baseClass;
    myDefinitions = definitions;
    myUniverse = universe;
  }

  @Override
  public ClassDefinition getBaseClass() {
    return myBaseClass;
  }

  @Override
  public Collection<OverriddenDefinition> getDefinitions() {
    return myDefinitions.values();
  }

  public Map<FunctionDefinition, OverriddenDefinition> getDefinitionsMap() {
    return myDefinitions;
  }

  public Universe getUniverse() {
    return myUniverse;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitClassExt(this);
  }

  @Override
  public UniverseExpression getType(List<Binding> context) {
    return new UniverseExpression(myUniverse);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassExt(this, params);
  }
}
