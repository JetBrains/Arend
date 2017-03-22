package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.Set;

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
  public TypeExpression subst(LevelSubstitution substitution) {
    return substitution.isEmpty() ? this : new TypeExpression(myType.subst(substitution), mySort.subst(substitution));
  }

  @Override
  public Type strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    Expression expr = myType.strip(bounds, errorReporter);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort);
  }
}
