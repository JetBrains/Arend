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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassCallExpression extends DefCallExpression implements Type {
  private final Sort mySortArgument;
  private final ReadonlyFieldSet myFieldSet;

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument) {
    super(definition);
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
    return myFieldSet.getImplemented().stream().filter(entry -> !getDefinition().getFieldSet().isImplemented(entry.getKey())).collect(Collectors.toSet());
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
    return new NormalizeVisitor().visitClassCall(this, mode);
  }

  public Sort getSort() {
    return myFieldSet.getSort();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }
}
