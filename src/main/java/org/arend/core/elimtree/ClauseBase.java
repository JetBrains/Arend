package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.core.pattern.Pattern;

import java.util.List;

public class ClauseBase {
  public final List<Pattern> patterns;
  public final Expression expression;

  public ClauseBase(List<Pattern> patterns, Expression expression) {
    this.patterns = patterns;
    this.expression = expression;
  }
}
