package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.Collections;
import java.util.Map;

public class ClassCallExpression extends DefCallExpression {
  private final Map<ClassField, ImplementStatement> myStatements;
  private Universe myUniverse;

  public ClassCallExpression(ClassDefinition definition) {
    super(definition);
    myStatements = Collections.emptyMap();
    myUniverse = definition.getUniverse();
  }

  public ClassCallExpression(ClassDefinition definition, Map<ClassField, ImplementStatement> statements) {
    super(definition);
    myStatements = statements;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    ClassField parent = getDefinition().getParentField();
    myStatements.put(parent, new ImplementStatement(null, thisExpr));
    return this;
  }

  @Override
  public ClassDefinition getDefinition() {
    return (ClassDefinition) super.getDefinition();
  }

  public Map<ClassField, ImplementStatement> getImplementStatements() {
    return myStatements;
  }

  public Universe getUniverse() {
    if (myUniverse == null) {
      myUniverse = new Universe.Type(0, Universe.Type.PROP);
      for (ClassField field : getDefinition().getFields()) {
        if (!myStatements.containsKey(field)) {
          Expression expr = field.getBaseType().getType().normalize(NormalizeVisitor.Mode.WHNF);
          myUniverse = myUniverse.max(expr instanceof UniverseExpression ? ((UniverseExpression) expr).getUniverse() : field.getUniverse());
          assert expr instanceof UniverseExpression;
        }
      }
    }

    return myUniverse;
  }

  @Override
  public UniverseExpression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }

  public static class ImplementStatement {
    public Expression type;
    public Expression term;

    public ImplementStatement(Expression type, Expression term) {
      this.type = type;
      this.term = term;
    }
  }
}
