package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.HoleExpression;

public class CompareVisitor implements AbstractExpressionVisitor<Abstract.Expression, CompareVisitor.Result> {
  public static enum Result { OK, NOT_OK, MAYBE_OK }

  public static Result and(Result a, Result b) {
    switch (a) {
      case OK:
        return b;
      case NOT_OK:
        return Result.NOT_OK;
      case MAYBE_OK:
        if (b == Result.NOT_OK) {
          return Result.NOT_OK;
        } else {
          return Result.MAYBE_OK;
        }
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    if (!(other instanceof Abstract.AppExpression)) return Result.NOT_OK;
    Abstract.AppExpression otherApp = (Abstract.AppExpression) other;
    return and(expr.getFunction().accept(this, otherApp.getFunction()), expr.getArgument().accept(this, otherApp.getArgument()));
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    if (!(other instanceof Abstract.DefCallExpression)) return Result.NOT_OK;
    return expr.getDefinition().equals(((Abstract.DefCallExpression) other).getDefinition()) ? Result.OK : Result.NOT_OK;
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    if (!(other instanceof Abstract.IndexExpression)) return Result.NOT_OK;
    return expr.getIndex() == ((Abstract.IndexExpression) other).getIndex() ? Result.OK : Result.NOT_OK;
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    if (!(other instanceof Abstract.LamExpression)) return Result.NOT_OK;
    return expr.getBody().accept(this, ((Abstract.LamExpression) other).getBody());
  }

  @Override
  public Result visitNat(Abstract.NatExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    return other instanceof Abstract.NatExpression ? Result.OK : Result.NOT_OK;
  }

  @Override
  public Result visitNelim(Abstract.NelimExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    return other instanceof Abstract.NelimExpression ? Result.OK : Result.NOT_OK;
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    if (!(other instanceof Abstract.PiExpression)) return Result.NOT_OK;
    Abstract.PiExpression otherPi = (Abstract.PiExpression) other;
    return and(expr.getDomain().accept(this, otherPi.getDomain()), expr.getCodomain().accept(this, otherPi.getCodomain()));
  }

  @Override
  public Result visitSuc(Abstract.SucExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    return other instanceof Abstract.SucExpression ? Result.OK : Result.NOT_OK;
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    if (!(other instanceof Abstract.UniverseExpression)) return Result.NOT_OK;
    return expr.getLevel() == -1 || expr.getLevel() >= ((Abstract.UniverseExpression) other).getLevel() ? Result.OK : Result.NOT_OK;
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    if (!(other instanceof Abstract.VarExpression)) return Result.NOT_OK;
    return expr.getName().equals(((Abstract.VarExpression) other).getName()) ? Result.OK : Result.NOT_OK;
  }

  @Override
  public Result visitZero(Abstract.ZeroExpression expr, Abstract.Expression other) {
    if (expr == other) return Result.OK;
    if (other instanceof HoleExpression) return Result.MAYBE_OK;
    return other instanceof Abstract.ZeroExpression ? Result.OK : Result.NOT_OK;
  }

  @Override
  public Result visitHole(Abstract.HoleExpression expr, Abstract.Expression other) {
    return Result.MAYBE_OK;
  }
}
