package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

public class TypeExpression {
  private final Expression myType;
  private final Sort mySort;

  public TypeExpression(Expression type, Sort sort) {
    myType = type;
    mySort = sort;
  }

  public Expression getExpr() {
    return myType;
  }

  public Sort getSort() {
    return mySort;
  }
}
