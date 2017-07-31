package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

public class TypeExpression implements Type {
  private final Expression myType;
  private final Sort mySort;

  public TypeExpression(Expression type, Sort sort) {
    myType = type;
    mySort = sort;
  }

  @Override
  public Expression getExpr() {
    return myType;
  }

  @Override
  public Sort getSortOfType() {
    return mySort;
  }

  @Override
  public Type subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    if (exprSubstitution.isEmpty() && levelSubstitution.isEmpty()) {
      return this;
    }
    Expression expr = myType.subst(exprSubstitution, levelSubstitution);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort.subst(levelSubstitution));
  }

  @Override
  public Type strip(LocalErrorReporter errorReporter) {
    Expression expr = myType.strip(errorReporter);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort);
  }

  @Override
  public Type normalize(NormalizeVisitor.Mode mode) {
    Expression expr = myType.normalize(mode);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort);
  }
}
