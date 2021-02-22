package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.util.Decision;

import java.util.ArrayList;
import java.util.List;

public class LeafElimTree extends ElimTree {
  private final List<Integer> myIndices;
  private final int myClauseIndex;

  public LeafElimTree(int skip, List<Integer> indices, int clauseIndex) {
    super(skip);
    myIndices = indices;
    myClauseIndex = clauseIndex;
  }

  public int getArgumentIndex(int index) {
    return myIndices == null ? index : myIndices.get(index);
  }

  public List<? extends Integer> getArgumentIndices() {
    return myIndices;
  }

  public int getClauseIndex() {
    return myClauseIndex;
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
  public List<Expression> normalizeArguments(List<? extends Expression> arguments) {
    return new ArrayList<>(arguments);
  }
}
