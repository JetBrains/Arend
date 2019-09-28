package org.arend.core.expr;

import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.util.Decision;

import java.util.Collections;
import java.util.List;

public class PEvalExpression extends Expression {
  private final Expression myExpression;

  public PEvalExpression(Expression expression) {
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public ElimTree getElimTree() {
    if (myExpression instanceof FunCallExpression) {
      Body body = ((FunCallExpression) myExpression).getDefinition().getActualBody();
      return body instanceof ElimTree ? (ElimTree) body : null;
    } else {
      return myExpression instanceof CaseExpression ? ((CaseExpression) myExpression).getElimTree() : null;
    }
  }

  public LevelSubstitution getLevelSubstitution() {
    return myExpression instanceof FunCallExpression ? ((FunCallExpression) myExpression).getSortArgument().toLevelSubstitution() : LevelSubstitution.EMPTY;
  }

  public List<? extends Expression> getArguments() {
    return myExpression instanceof FunCallExpression ? ((FunCallExpression) myExpression).getDefCallArguments() : myExpression instanceof CaseExpression ? ((CaseExpression) myExpression).getArguments() : Collections.emptyList();
  }

  public Expression eval() {
    ElimTree elimTree = getElimTree();
    return elimTree == null ? null : NormalizeVisitor.INSTANCE.eval(elimTree, getArguments(), new ExprSubstitution(), getLevelSubstitution());
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPEval(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
