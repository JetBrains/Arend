package org.arend.core.subst;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.expr.FunCallExpression;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.core.ops.NormalizationMode;

import java.util.Set;

public class UnfoldVisitor extends SubstVisitor {
  private final Set<? extends CoreDefinition> myDefinitions;
  private final Set<CoreDefinition> myUnfolded;

  public UnfoldVisitor(Set<? extends CoreDefinition> definitions, Set<CoreDefinition> unfolded) {
    super(new ExprSubstitution(), LevelSubstitution.EMPTY);
    myDefinitions = definitions;
    myUnfolded = unfolded;
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Void params) {
    if (expr.getDefinition().getBody() instanceof Expression && myDefinitions.contains(expr.getDefinition())) {
      if (myUnfolded != null) {
        myUnfolded.add(expr.getDefinition());
      }
      ExprSubstitution substitution = getExprSubstitution();
      DependentLink param = expr.getDefinition().getParameters();
      for (Expression argument : expr.getDefCallArguments()) {
        substitution.add(param, argument.accept(this, null));
        param = param.getNext();
      }
      Expression result = ((Expression) expr.getDefinition().getBody()).accept(new SubstVisitor(substitution, expr.getSortArgument().toLevelSubstitution().subst(getLevelSubstitution())), null);
      DependentLink.Helper.freeSubsts(expr.getDefinition().getParameters(), substitution);
      return result;
    } else {
      return super.visitFunCall(expr, params);
    }
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    if (!expr.getDefinition().isProperty() && myDefinitions.contains(expr.getDefinition())) {
      Expression type = expr.getArgument().getType();
      ClassCallExpression classCall = type == null ? null : type.normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
      if (classCall != null) {
        Expression impl = classCall.getImplementation(expr.getDefinition(), expr.getArgument());
        if (impl != null) {
          if (myUnfolded != null) {
            myUnfolded.add(expr.getDefinition());
          }
          return impl;
        }
      }
    }
    return super.visitFieldCall(expr, params);
  }
}
