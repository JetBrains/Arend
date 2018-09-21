package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.core.pattern.Pattern;
import org.arend.term.concrete.Concrete;

import java.util.List;

public class Clause extends ClauseBase {
  public final Concrete.FunctionClause clause;

  public Clause(List<Pattern> patterns, Expression expression, Concrete.FunctionClause clause) {
    super(patterns, expression);
    this.clause = clause;
  }
}
