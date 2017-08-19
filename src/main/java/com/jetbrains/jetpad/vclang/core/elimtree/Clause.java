package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.List;

public class Clause<T> extends ClauseBase {
  public final Concrete.FunctionClause<T> clause;

  public Clause(List<Pattern> patterns, Expression expression, Concrete.FunctionClause<T> clause) {
    super(patterns, expression);
    this.clause = clause;
  }
}
