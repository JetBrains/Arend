package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.StripVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.HashMap;
import java.util.Map;

public class ClassCallExpression extends DefCallExpression implements Type {
  private final Sort mySortArgument;
  private final Map<ClassField, Expression> myImplementations;
  private final Sort mySort;

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument) {
    super(definition);
    mySortArgument = sortArgument;
    myImplementations = new HashMap<>();
    mySort = definition.getSort();
  }

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument, Map<ClassField, Expression> implementations, Sort sort) {
    super(definition);
    mySortArgument = sortArgument;
    myImplementations = implementations;
    mySort = sort;
  }

  public Map<ClassField, Expression> getImplementedHere() {
    return myImplementations;
  }

  public Expression getImplementationHere(ClassField field) {
    return myImplementations.get(field);
  }

  public Expression getImplementation(ClassField field, Expression thisExpr) {
    Expression expr = myImplementations.get(field);
    if (expr != null) {
      return expr;
    }
    FieldSet.Implementation impl = getDefinition().getFieldSet().getImplementation(field);
    return impl == null ? null : impl.substThisParam(thisExpr);
  }

  public boolean isImplemented(ClassField field) {
    return myImplementations.containsKey(field) || getDefinition().getFieldSet().isImplemented(field);
  }

  public boolean isUnit() {
    FieldSet fieldSet = getDefinition().getFieldSet();
    return myImplementations.size() + fieldSet.getImplemented().size() == fieldSet.getFields().size();
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
  public ClassCallExpression strip(LocalErrorReporter errorReporter) {
    return new StripVisitor(errorReporter).visitClassCall(this, null);
  }

  @Override
  public ClassCallExpression normalize(NormalizeVisitor.Mode mode) {
    return NormalizeVisitor.INSTANCE.visitClassCall(this, mode);
  }

  public Sort getSort() {
    return mySort;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }
}
