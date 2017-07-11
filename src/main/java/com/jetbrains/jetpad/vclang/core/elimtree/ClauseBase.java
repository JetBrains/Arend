package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.List;

public class ClauseBase {
  public final List<Pattern> patterns;
  public final Expression expression;

  public ClauseBase(List<Pattern> patterns, Expression expression) {
    this.patterns = patterns;
    this.expression = expression;
  }
}
