package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.internal.ReadonlyFieldSet;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassCallExpression extends DefCallExpression {
  private final LevelArguments myLevelArguments;
  private final ReadonlyFieldSet myFieldSet;

  public ClassCallExpression(ClassDefinition definition, LevelArguments levelArguments) {
    super(definition);
    assert definition.status().headerIsOK();
    myLevelArguments = levelArguments;
    myFieldSet = definition.getFieldSet();
  }

  public ClassCallExpression(ClassDefinition definition, LevelArguments levelArguments, ReadonlyFieldSet fieldSet) {
    super(definition);
    myLevelArguments = levelArguments;
    myFieldSet = fieldSet;
  }

  public ReadonlyFieldSet getFieldSet() {
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

  @Override
  public LevelArguments getLevelArguments() {
    return myLevelArguments;
  }

  public Sort getSort() {
    Sort sort = myFieldSet.getSort();
    if (sort == null) {
      myFieldSet.updateSorts(this);
      return myFieldSet.getSort();
    } else {
      return sort;
    }
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }

  @Override
  public ClassCallExpression toClassCall() {
    return this;
  }

  @Override
  public Expression addArgument(Expression argument) {
    throw new IllegalStateException();
  }
}
