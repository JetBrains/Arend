package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class RowImplicitArgsInference extends BaseImplicitArgsInference {
  public RowImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  abstract CheckTypeVisitor.Result inferRow(CheckTypeVisitor.OKResult fun, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression funExpr, Abstract.Expression expr);

  private CheckTypeVisitor.Result typeCheckFunctionApps(Abstract.Expression fun, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression expression) {
    CheckTypeVisitor.Result function;
    if (fun instanceof Abstract.DefCallExpression) {
      function = myVisitor.getTypeCheckingDefCall().typeCheckDefCall((Abstract.DefCallExpression) fun);
    } else {
      function = myVisitor.typeCheck(fun, null);
    }
    if (function instanceof CheckTypeVisitor.OKResult) {
      return inferRow((CheckTypeVisitor.OKResult) function, args, expectedType, fun, expression);
    }

    if (function instanceof CheckTypeVisitor.InferErrorResult) {
      myVisitor.getErrorReporter().report(((CheckTypeVisitor.InferErrorResult) function).error);
    }
    for (Abstract.ArgumentExpression arg : args) {
      myVisitor.typeCheck(arg.getExpression(), null);
    }
    return null;
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.AppExpression expr, Expression expectedType) {
    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    return typeCheckFunctionApps(Abstract.getFunction(expr, args), args, expectedType, expr);
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.BinOpExpression expr, Expression expectedType) {
    class AbstractArgumentExpression implements Abstract.ArgumentExpression {
      Abstract.Expression expression;

      public AbstractArgumentExpression(Abstract.Expression expression) {
        this.expression = expression;
      }

      @Override
      public Abstract.Expression getExpression() {
        return expression;
      }

      @Override
      public boolean isExplicit() {
        return true;
      }

      @Override
      public boolean isHidden() {
        return false;
      }

      @Override
      public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
        expression.prettyPrint(builder, names, prec);
      }
    }

    List<Abstract.ArgumentExpression> args = new ArrayList<>(2);
    args.add(new AbstractArgumentExpression(expr.getLeft()));
    args.add(new AbstractArgumentExpression(expr.getRight()));

    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : null;
    return typeCheckFunctionApps(new Concrete.DefCallExpression(position, expr.getResolvedBinOpName()), args, expectedType, expr);
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.OKResult fun, Expression expectedType, Abstract.Expression expr) {
    return inferRow(fun, new ArrayList<Abstract.ArgumentExpression>(0), expectedType, expr, expr);
  }
}
