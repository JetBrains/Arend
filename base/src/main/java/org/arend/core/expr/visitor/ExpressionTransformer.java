package org.arend.core.expr.visitor;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.IntegerExpression;
import org.arend.core.sort.Sort;
import org.arend.prelude.Prelude;

import java.util.ArrayList;
import java.util.List;

import static org.arend.core.expr.ExpressionFactory.Suc;

public abstract class ExpressionTransformer<P> extends BaseExpressionVisitor<P, Expression> {
  protected Expression visit(Expression expr, P params) {
    return expr.accept(this, params);
  }

  protected Expression visitDataTypeArgument(Expression expr, P params) {
    return visit(expr, params);
  }

  protected Expression preVisitConCall(ConCallExpression expr, P params) {
    return null;
  }

  protected Expression makeConCall(Constructor constructor, Sort sortArgument, List<Expression> dataTypeArguments, List<Expression> arguments) {
    return new ConCallExpression(constructor, sortArgument, dataTypeArguments, arguments);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, P params) {
    Expression it = expr;
    if (expr.getDefinition() == Prelude.SUC) {
      int n = 0;
      do {
        n++;
        it = (((ConCallExpression) it).getDefCallArguments()).get(0);
      } while (it instanceof ConCallExpression && ((ConCallExpression) it).getDefinition() == Prelude.SUC);

      it = visit(it, params);
      if (it instanceof IntegerExpression) {
        return ((IntegerExpression) it).plus(n);
      }

      for (int i = 0; i < n; i++) {
        it = Suc(it);
      }
      return it;
    }

    Expression result = null;
    List<Expression> args = null;
    int recursiveParam = -1;
    do {
      ConCallExpression conCall = (ConCallExpression) it;
      it = preVisitConCall(conCall, params);
      if (it != null) {
        if (args != null) {
          args.set(recursiveParam, it);
          return result;
        } else {
          return it;
        }
      }

      List<Expression> dataTypeArgs = new ArrayList<>(conCall.getDataTypeArguments().size());
      for (Expression arg : conCall.getDataTypeArguments()) {
        Expression newArg = visitDataTypeArgument(arg, params);
        if (newArg == null) {
          return null;
        }
        dataTypeArgs.add(newArg);
      }

      List<Expression> newArgs = new ArrayList<>();
      it = makeConCall(conCall.getDefinition(), conCall.getSortArgument(), dataTypeArgs, newArgs);
      if (args != null) {
        args.set(recursiveParam, it);
      } else {
        result = it;
      }
      args = newArgs;

      recursiveParam = conCall.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        for (Expression arg : conCall.getDefCallArguments()) {
          Expression newArg = visit(arg, params);
          if (newArg == null) {
            return null;
          }
          args.add(newArg);
        }
        return result;
      }

      for (int i = 0; i < conCall.getDefCallArguments().size(); i++) {
        if (i != recursiveParam) {
          Expression newArg = visit(conCall.getDefCallArguments().get(i), params);
          if (newArg == null) {
            return null;
          }
          args.add(newArg);
        } else {
          args.add(null);
        }
      }

      it = conCall.getDefCallArguments().get(recursiveParam);
    } while (it instanceof ConCallExpression);

    Expression newArg = visit(it, params);
    if (newArg == null) {
      return null;
    }
    args.set(recursiveParam, it.accept(this, null));
    return result;
  }
}
