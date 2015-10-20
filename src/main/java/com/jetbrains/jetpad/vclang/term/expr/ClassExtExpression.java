package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.statement.DefineStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassExtExpression extends Expression implements Abstract.ClassExtExpression {
  private final ClassCallExpression myBaseClassExpression;
  private final Map<FunctionDefinition, OverriddenDefinition> myDefinitions;
  private final Universe myUniverse;

  public ClassExtExpression(ClassCallExpression baseClassExpression, Map<FunctionDefinition, OverriddenDefinition> definitions, Universe universe) {
    myBaseClassExpression = baseClassExpression;
    myDefinitions = definitions;
    myUniverse = universe;
  }

  @Override
  public ClassCallExpression getBaseClassExpression() {
    return myBaseClassExpression;
  }

  public ClassDefinition getBaseClass() {
    return myBaseClassExpression.getDefinition();
  }

  @Override
  public List<DefineStatement> getStatements() {
    List<DefineStatement> statements = new ArrayList<>(myDefinitions.size());
    for (OverriddenDefinition overriddenDefinition : myDefinitions.values()) {
      statements.add(new DefineStatement(overriddenDefinition, false));
    }
    return statements;
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
