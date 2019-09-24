package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.util.Decision;

import java.util.List;

public class ElimBody implements Body {
  private final List<? extends ElimClause> myClauses;
  private final ElimTree myElimTree;

  public ElimBody(List<? extends ElimClause> clauses, ElimTree elimTree) {
    myClauses = clauses;
    myElimTree = elimTree;
  }

  public List<? extends ElimClause> getClauses() {
    return myClauses;
  }

  public ElimClause getClause(int index) {
    return myClauses.get(index);
  }

  public ElimTree getElimTree() {
    return myElimTree;
  }

  @Override
  public Decision isWHNF(List<? extends Expression> arguments) {
    return myElimTree.isWHNF(arguments);
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    return myElimTree.getStuckExpression(arguments, expression);
  }
}
