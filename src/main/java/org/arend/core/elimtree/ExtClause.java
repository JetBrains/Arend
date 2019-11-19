package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.core.pattern.Pattern;
import org.arend.core.subst.ExprSubstitution;
import org.arend.term.concrete.Concrete;

import java.util.List;

public class ExtClause extends Clause {
  public final ExprSubstitution substitution;

  public ExtClause(List<Pattern> patterns, Expression expression, ExprSubstitution substitution, Concrete.FunctionClause clause) {
    super(patterns, expression, clause);
    this.substitution = substitution;
  }
}
