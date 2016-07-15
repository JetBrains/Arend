package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMaxSet;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.Collections;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Lam;

public class ClassCallExpression extends DefCallExpression {
  private final Map<ClassField, ImplementStatement> myStatements;
  private SortMaxSet mySorts;

  public ClassCallExpression(ClassDefinition definition) {
    super(definition);
    myStatements = Collections.emptyMap();
    mySorts = definition.getSorts();
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

  public SortMaxSet getSorts() {
    if (mySorts == null) {
      ExprSubstitution substitution = getDefinition().getImplementedFields();
      for (Map.Entry<ClassField, ImplementStatement> entry : myStatements.entrySet()) {
        if (entry.getValue().term != null) {
          substitution.add(entry.getKey(), Lam(entry.getKey().getThisParameter(), entry.getValue().term));
        }
      }

      mySorts = new SortMaxSet();
      for (ClassField field : getDefinition().getFields()) {
        if (!field.hasErrors() && !getDefinition().getFieldImpl(field).isImplemented() && !myStatements.containsKey(field)) {
          field.updateSort(mySorts, substitution);
        }
      }
    }

    return mySorts;
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
