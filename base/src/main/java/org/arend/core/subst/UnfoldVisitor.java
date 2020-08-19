package org.arend.core.subst;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FunCallExpression;
import org.arend.ext.core.definition.CoreFunctionDefinition;

import java.util.Set;

public class UnfoldVisitor extends SubstVisitor {
  private final Set<? extends CoreFunctionDefinition> myFunctions;
  private final Set<CoreFunctionDefinition> myUnfolded;

  public UnfoldVisitor(Set<? extends CoreFunctionDefinition> functions, Set<CoreFunctionDefinition> unfolded) {
    super(new ExprSubstitution(), LevelSubstitution.EMPTY);
    myFunctions = functions;
    myUnfolded = unfolded;
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Void params) {
    if (expr.getDefinition().getBody() instanceof Expression && myFunctions.contains(expr.getDefinition())) {
      if (myUnfolded != null) {
        myUnfolded.add(expr.getDefinition());
      }
      ExprSubstitution substitution = getExprSubstitution();
      DependentLink param = expr.getDefinition().getParameters();
      for (Expression argument : expr.getDefCallArguments()) {
        substitution.add(param, argument.accept(this, null));
        param = param.getNext();
      }
      Expression result = (Expression) expr.getDefinition().getBody();
      DependentLink.Helper.freeSubsts(expr.getDefinition().getParameters(), substitution);
      return result;
    } else {
      return super.visitFunCall(expr, params);
    }
  }
}
