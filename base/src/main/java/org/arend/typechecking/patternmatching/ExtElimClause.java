package org.arend.typechecking.patternmatching;

import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.Expression;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.subst.ExprSubstitution;

import java.util.List;

public class ExtElimClause extends ElimClause<ExpressionPattern> {
  private final ExprSubstitution mySubstitution; // Substitutes in getPatterns() expressions that were matched in idp patterns

  public ExtElimClause(List<ExpressionPattern> patterns, Expression expression, ExprSubstitution substitution) {
    super(patterns, expression);
    mySubstitution = substitution;
  }

  public ExprSubstitution getSubstitution() {
    return mySubstitution;
  }
}
