package org.arend.core.expr.visitor;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ExpressionFactory;
import org.arend.core.expr.IntegerExpression;
import org.arend.core.subst.Levels;
import org.arend.prelude.Prelude;

import java.util.ArrayList;
import java.util.List;

public abstract class ExpressionTransformer<P> extends BaseExpressionVisitor<P, Expression> {
  protected boolean preserveOrder() {
    return false;
  }

  protected Expression visit(Expression expr, P params) {
    return expr.accept(this, params);
  }

  protected Expression visitDataTypeArgument(Expression expr, P params) {
    return visit(expr, params);
  }

  protected List<Expression> visitDataTypeArguments(List<? extends Expression> args, P params) {
    List<Expression> dataTypeArgs = new ArrayList<>(args.size());
    for (Expression arg : args) {
      Expression newArg = visitDataTypeArgument(arg, params);
      if (newArg == null) {
        return null;
      }
      dataTypeArgs.add(newArg);
    }
    return dataTypeArgs;
  }

  protected Expression preVisitConCall(ConCallExpression expr, P params) {
    return null;
  }

  protected ConCallExpression makeConCall(Constructor constructor, Levels levels, List<Expression> dataTypeArguments, List<Expression> arguments) {
    return new ConCallExpression(constructor, levels, dataTypeArguments, arguments);
  }

  protected boolean useStack(ConCallExpression expression) {
    return preserveOrder() && expression.getDefinition().getRecursiveParameter() >= 0 && expression.getDefinition().getRecursiveParameter() < expression.getDefCallArguments().size() - 1;
  }

  private Expression visitConCallWithStack(ConCallExpression expr, P params) {
    int recursiveParam = expr.getDefinition().getRecursiveParameter();
    List<ConCallExpression> argStack = new ArrayList<>();
    List<ConCallExpression> resultStack = new ArrayList<>();
    while (true) {
      List<Expression> dataTypeArgs = visitDataTypeArguments(expr.getDataTypeArguments(), params);
      if (dataTypeArgs == null) return null;
      List<Expression> args = new ArrayList<>();
      ConCallExpression result = makeConCall(expr.getDefinition(), expr.getLevels(), dataTypeArgs, args);
      for (int i = 0; i < recursiveParam; i++) {
        Expression newArg = visit(expr.getDefCallArguments().get(i), params);
        if (newArg == null) {
          return null;
        }
        args.add(newArg);
      }

      argStack.add(expr);
      resultStack.add(result);

      Expression rec = expr.getDefCallArguments().get(recursiveParam);
      if (!(rec instanceof ConCallExpression && ((ConCallExpression) rec).getDefinition().getRecursiveParameter() >= 0)) {
        Expression newArg = visit(rec, params);
        if (newArg == null) {
          return null;
        }
        args.add(newArg);
        break;
      }

      expr = (ConCallExpression) rec;
      recursiveParam = expr.getDefinition().getRecursiveParameter();
    }

    for (int i = argStack.size() - 1; i >= 0; i--) {
      expr = argStack.get(i);
      ConCallExpression result = resultStack.get(i);
      for (int j = expr.getDefinition().getRecursiveParameter() + 1; j < expr.getDefCallArguments().size(); j++) {
        Expression newArg = visit(expr.getDefCallArguments().get(j), params);
        if (newArg == null) {
          return null;
        }
        result.getDefCallArguments().add(newArg);
      }
    }

    return resultStack.get(0);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, P params) {
    Expression it = expr;
    if (expr.getDefinition() == Prelude.SUC) {
      int n = 0;
      Expression result;
      do {
        result = preVisitConCall((ConCallExpression) it, params);
        if (result != null) {
          it = result;
          break;
        }
        n++;
        it = (((ConCallExpression) it).getDefCallArguments()).get(0);
      } while (it instanceof ConCallExpression && ((ConCallExpression) it).getDefinition() == Prelude.SUC);

      if (result == null) {
        it = visit(it, params);
        if (it == null) {
          return null;
        }
      }

      if (it instanceof IntegerExpression) {
        return ((IntegerExpression) it).plus(n);
      }

      return ExpressionFactory.add(it, n);
    }

    if (useStack(expr)) {
      return visitConCallWithStack(expr, params);
    }

    int recursiveParam = expr.getDefinition().getRecursiveParameter();
    Expression result = null;
    List<Expression> args = null;
    while (true) {
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

      List<Expression> dataTypeArgs = visitDataTypeArguments(conCall.getDataTypeArguments(), params);
      if (dataTypeArgs == null) return null;
      List<Expression> newArgs = new ArrayList<>();
      it = makeConCall(conCall.getDefinition(), conCall.getLevels(), dataTypeArgs, newArgs);
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
      if (!(it instanceof ConCallExpression)) {
        break;
      }
      if (useStack((ConCallExpression) it)) {
        return visitConCallWithStack((ConCallExpression) it, params);
      }
    }

    Expression newArg = visit(it, params);
    if (newArg == null) {
      return null;
    }
    args.set(recursiveParam, newArg);
    return result;
  }
}
