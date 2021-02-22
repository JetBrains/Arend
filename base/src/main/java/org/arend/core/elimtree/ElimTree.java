package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.util.Decision;

import java.util.List;

public abstract class ElimTree {
  private final int mySkip;

  public ElimTree(int skip) {
    mySkip = skip;
  }

  public int getSkip() {
    return mySkip;
  }

  public abstract Decision isWHNF(List<? extends Expression> arguments);

  public abstract Expression getStuckExpression(List<? extends Expression> arguments, Expression expression);

  public abstract List<Expression> normalizeArguments(List<? extends Expression> arguments);
}
