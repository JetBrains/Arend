package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
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

  public Body getBody() {
    if (myExpression instanceof FunCallExpression) {
      return ((FunCallExpression) myExpression).getDefinition().getActualBody();
    } else {
      return myExpression instanceof CaseExpression ? ((CaseExpression) myExpression).getElimTree() : null;
    }
  }

  public DependentLink getParameters() {
    if (myExpression instanceof FunCallExpression) {
      return ((FunCallExpression) myExpression).getDefinition().getParameters();
    } else {
      return myExpression instanceof CaseExpression ? ((CaseExpression) myExpression).getParameters() : EmptyDependentLink.getInstance();
    }
  }

  public LevelSubstitution getLevelSubstitution() {
    return myExpression instanceof FunCallExpression ? ((FunCallExpression) myExpression).getSortArgument().toLevelSubstitution() : LevelSubstitution.EMPTY;
  }

  public List<? extends Expression> getArguments() {
    return myExpression instanceof FunCallExpression ? ((FunCallExpression) myExpression).getDefCallArguments() : myExpression instanceof CaseExpression ? ((CaseExpression) myExpression).getArguments() : Collections.emptyList();
  }

  public Expression eval() {
    return NormalizeVisitor.INSTANCE.eval(myExpression);
  }

  @Override
  public boolean canBeConstructor() {
    return false;
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
