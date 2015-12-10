package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
      List<Binding> context = new ArrayList<>(1);
      context.add(new TypedBinding("\\this", this));
      for (ClassField field : getDefinition().getFields()) {
        if (!myStatements.containsKey(field)) {
          Expression expr = field.getBaseType().normalize(NormalizeVisitor.Mode.NF, context).getType(context).normalize(NormalizeVisitor.Mode.WHNF, context);
          myUniverse = myUniverse.max(expr instanceof UniverseExpression ? ((UniverseExpression) expr).getUniverse() : field.getUniverse());
          assert expr instanceof UniverseExpression;
        }
      }
    }

    return myUniverse;
  }

  @Override
  public UniverseExpression getType(List<Binding> context) {
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
