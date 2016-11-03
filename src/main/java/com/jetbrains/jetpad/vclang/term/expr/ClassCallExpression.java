package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelArguments;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ClassCall;

public class ClassCallExpression extends DefCallExpression {
  private final FieldSet myFieldSet;

  public ClassCallExpression(ClassDefinition definition, LevelArguments polyParams) {
    super(definition, polyParams);
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
    return myFieldSet.getSorts(this);
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    FieldSet newFieldSet = new FieldSet(myFieldSet);
    ClassField parent = getDefinition().getEnclosingThisField();
    boolean success = newFieldSet.implementField(parent, new FieldSet.Implementation(null, thisExpr), this);
    assert success;
    return ClassCall(getDefinition(), getPolyArguments(), newFieldSet);
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
    newFieldSet.addFieldsFrom(getFieldSet(), this);
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : getFieldSet().getImplemented()) {
      if (getDefinition().getFieldSet().isImplemented(entry.getKey())) {
        newFieldSet.implementField(entry.getKey(), entry.getValue(), this);
      } else {
        newFieldSet.implementField(entry.getKey(), new FieldSet.Implementation(entry.getValue().thisParam, entry.getValue().term.accept(visitor, arg)), this);
      }
    }
    return this instanceof ClassViewCallExpression ? new ClassViewCallExpression(getDefinition(), getPolyArguments(), newFieldSet, ((ClassViewCallExpression) this).getClassView()) : ClassCall(getDefinition(), getPolyArguments(), newFieldSet);
  }

  @Override
  public Expression addArgument(Expression argument) {
    throw new IllegalStateException();
  }
}
