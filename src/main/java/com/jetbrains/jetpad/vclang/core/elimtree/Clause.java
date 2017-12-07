package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.List;

public class Clause extends ClauseBase {
  public final Concrete.FunctionClause clause;

  public Clause(List<Pattern> patterns, Expression expression, Concrete.FunctionClause clause) {
    super(patterns, expression);
    this.clause = clause;
  }
}
