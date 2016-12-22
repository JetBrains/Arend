package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class RowImplicitArgsInference extends BaseImplicitArgsInference {
  public RowImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  abstract CheckTypeVisitor.Result inferRow(CheckTypeVisitor.PreResult fun, List<Abstract.ArgumentExpression> args, Abstract.Expression funExpr, Abstract.Expression expr);

  private CheckTypeVisitor.PreResult typeCheckFunctionApps(Abstract.Expression fun, List<Abstract.ArgumentExpression> args, Abstract.Expression expression) {
    CheckTypeVisitor.PreResult function;
    if (fun instanceof Abstract.DefCallExpression) {
      function = myVisitor.getTypeCheckingDefCall().typeCheckDefCall((Abstract.DefCallExpression) fun);
    } else {
      function = myVisitor.typeCheck(fun, null);
    }
    if (function != null) {
      return inferRow(function, args, fun, expression);
    }

    for (Abstract.ArgumentExpression arg : args) {
      myVisitor.typeCheck(arg.getExpression(), null);
    }
    return null;
  }

  @Override
  public CheckTypeVisitor.PreResult infer(Abstract.AppExpression expr, Type expectedType) {
    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    return typeCheckFunctionApps(Abstract.getFunction(expr, args), args, expr);
  }

  @Override
  public CheckTypeVisitor.PreResult infer(Abstract.BinOpExpression expr, Type expectedType) {
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
    }

    List<Abstract.ArgumentExpression> args = new ArrayList<>(2);
    args.add(new AbstractArgumentExpression(expr.getLeft()));
    args.add(new AbstractArgumentExpression(expr.getRight()));

    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : null;
    return typeCheckFunctionApps(new Concrete.DefCallExpression(position, expr.getResolvedBinOp()), args, expr);
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result fun, Type expectedType, Abstract.Expression expr) {
    return myVisitor.checkResult(expectedType, inferRow(fun, new ArrayList<Abstract.ArgumentExpression>(0), expr, expr), expr);
  }
}
