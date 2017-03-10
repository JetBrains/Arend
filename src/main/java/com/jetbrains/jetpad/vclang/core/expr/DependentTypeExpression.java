package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

public abstract class DependentTypeExpression extends Expression {
  private final Sort mySort;
  private final DependentLink myLink;

  public DependentTypeExpression(Sort sort, DependentLink link) {
    mySort = sort;
    myLink = link;
  }

  public Sort getSort() {
    return mySort;
  }

  public DependentLink getParameters() {
    return myLink;
  }
}
