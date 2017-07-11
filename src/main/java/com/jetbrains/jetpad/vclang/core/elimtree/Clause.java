package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class Clause extends ClauseBase {
  public final Abstract.FunctionClause clause;

  public Clause(List<Pattern> patterns, Expression expression, Abstract.FunctionClause clause) {
    super(patterns, expression);
    this.clause = clause;
  }
}
