package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.pattern.Pattern;
import org.arend.ext.core.body.CoreElimBody;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ElimBody implements Body, CoreElimBody {
  private final List<ElimClause<Pattern>> myClauses;
  private final ElimTree myElimTree;

  public ElimBody(List<ElimClause<Pattern>> clauses, ElimTree elimTree) {
    myElimTree = elimTree;
    myClauses = clauses;
  }

  @Override
  @NotNull
  public final List<? extends ElimClause<Pattern>> getClauses() {
    return myClauses;
  }

  public final ElimTree getElimTree() {
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

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof ElimBody && CompareVisitor.compare(DummyEquations.getInstance(), this, (ElimBody) other, null);
  }
}
