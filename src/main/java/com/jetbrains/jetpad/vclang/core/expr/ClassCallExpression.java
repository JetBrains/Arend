package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.ClassCall;

public class ClassCallExpression extends DefCallExpression {
  private final FieldSet myFieldSet;

  public ClassCallExpression(ClassDefinition definition, LevelArguments polyParams) {
    super(definition, polyParams);
    assert definition.status().headerIsOK();
    myFieldSet = new FieldSet(definition.getFieldSet());
  }

  public ClassCallExpression(ClassDefinition definition, LevelArguments polyParams, FieldSet fieldSet) {
    super(definition, polyParams);
    myFieldSet = fieldSet;
  }

  public FieldSet getFieldSet() {
    return myFieldSet;
  }

  public Collection<Map.Entry<ClassField, FieldSet.Implementation>> getImplementedHere() {
    // TODO[java8]: turn into a stream
    Set<Map.Entry<ClassField, FieldSet.Implementation>> result = new HashSet<>();
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : myFieldSet.getImplemented()) {
      if (getDefinition().getFieldSet().isImplemented(entry.getKey())) continue;
      result.add(entry);
    }
    return result;
  }

  @Override
  public ClassDefinition getDefinition() {
    return (ClassDefinition) super.getDefinition();
  }

  public SortMax getSorts() {
    myFieldSet.updateSorts(this);
    return myFieldSet.getSorts();
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    FieldSet newFieldSet = new FieldSet(myFieldSet);
    ClassField parent = getDefinition().getEnclosingThisField();
    boolean success = newFieldSet.implementField(parent, new FieldSet.Implementation(null, thisExpr));
    assert success;
    return ClassCall(getDefinition(), getLevelArguments(), newFieldSet);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }

  @Override
  public ClassCallExpression toClassCall() {
    return this;
  }

  public <P> ClassCallExpression applyVisitorToImplementedHere(ExpressionVisitor<P, Expression> visitor, P arg) {
    FieldSet newFieldSet = new FieldSet();
    newFieldSet.addFieldsFrom(getFieldSet());
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : getFieldSet().getImplemented()) {
      if (getDefinition().getFieldSet().isImplemented(entry.getKey())) {
        newFieldSet.implementField(entry.getKey(), entry.getValue());
      } else {
        newFieldSet.implementField(entry.getKey(), new FieldSet.Implementation(entry.getValue().thisParam, entry.getValue().term.accept(visitor, arg)));
      }
    }
    return ClassCall(getDefinition(), getLevelArguments(), newFieldSet);
  }

  @Override
  public Expression addArgument(Expression argument) {
    throw new IllegalStateException();
  }
}
