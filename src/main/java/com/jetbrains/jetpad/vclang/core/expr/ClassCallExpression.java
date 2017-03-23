package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.StripVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.internal.ReadonlyFieldSet;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.normalization.EvalNormalizer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassCallExpression extends DefCallExpression implements Type {
  private final Sort mySortArgument;
  private final ReadonlyFieldSet myFieldSet;

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument) {
    super(definition);
    assert definition.status().headerIsOK();
    mySortArgument = sortArgument;
    myFieldSet = definition.getFieldSet();
  }

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument, ReadonlyFieldSet fieldSet) {
    super(definition);
    mySortArgument = sortArgument;
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
  public Sort getSortArgument() {
    return mySortArgument;
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return getSort();
  }

  @Override
  public ClassCallExpression subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    return new SubstVisitor(exprSubstitution, levelSubstitution).visitClassCall(this, null);
  }

  @Override
  public ClassCallExpression strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return new StripVisitor(bounds, errorReporter).visitClassCall(this, null);
  }

  @Override
  public ClassCallExpression normalize(NormalizeVisitor.Mode mode) {
    return new NormalizeVisitor(new EvalNormalizer()).visitClassCall(this, mode);
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
