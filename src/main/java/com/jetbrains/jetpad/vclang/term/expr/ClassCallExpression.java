package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Lam;

public class ClassCallExpression extends DefCallExpression {
  private final Map<ClassField, ImplementStatement> myStatements;
  private TypeUniverse myUniverse;

  public ClassCallExpression(ClassDefinition definition) {
    super(definition);
    myStatements = new HashMap<>();
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

  public TypeUniverse getUniverse() {
    if (myUniverse == null) {
      ExprSubstitution substitution = getDefinition().getImplementedFields();
      for (Map.Entry<ClassField, ImplementStatement> entry : myStatements.entrySet()) {
        if (entry.getValue().term != null) {
          substitution.add(entry.getKey(), Lam(entry.getKey().getThisParameter(), entry.getValue().term));
        }
      }

      myUniverse = TypeUniverse.PROP;
      for (ClassField field : getDefinition().getFields()) {
        if (!field.hasErrors() && !getDefinition().getFieldImpl(field).isImplemented() && !myStatements.containsKey(field)) {
          myUniverse = field.updateUniverse(myUniverse, substitution);
        }
      }
    }

    return myUniverse;
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

  @Override
  public ClassCallExpression toClassCall() {
    return this;
  }
}
