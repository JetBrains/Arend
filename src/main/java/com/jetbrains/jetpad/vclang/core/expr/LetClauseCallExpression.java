package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;

import java.util.List;

public class LetClauseCallExpression extends Expression implements CallableCallExpression {
  private final LetClause myLetClause;
  private final List<Expression> myArguments;

  public LetClauseCallExpression(LetClause letClause, List<Expression> arguments) {
    myLetClause = letClause;
    myArguments = arguments;
  }

  public LetClause getLetClause() {
    return myLetClause;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLetClauseCall(this, params);
  }

  @Override
  public LetClauseCallExpression toLetClauseCall() {
    return this;
  }

  @Override
  public Expression getStuckExpression() {
    Binding binding = ((BranchElimTreeNode) myLetClause.getElimTree()).getReference();
    int i = 0;
    for (SingleDependentLink link : myLetClause.getParameters()) {
      for (DependentLink param = link; param.hasNext(); param = param.getNext()) {
        if (param == binding) {
          return myArguments.get(i).getStuckExpression();
        }
      }
    }

    assert false;
    return null;
  }

  @Override
  public LetClause getDefinition() {
    return myLetClause;
  }
}
