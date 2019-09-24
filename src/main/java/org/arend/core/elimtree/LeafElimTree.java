package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.util.Decision;

import java.util.List;
import java.util.Objects;

public class LeafElimTree extends ElimTree {
  private final int myClause;

  public LeafElimTree(int skipped, int clause) {
    super(skipped);
    myClause = clause;
  }

  public int getClause() {
    return myClause;
  }

  @Override
  public Decision isWHNF(List<? extends Expression> arguments) {
    return Decision.NO;
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LeafElimTree that = (LeafElimTree) o;
    return skipped == that.skipped && myClause == that.myClause;
  }

  @Override
  public int hashCode() {
    return Objects.hash(skipped, myClause);
  }
}
