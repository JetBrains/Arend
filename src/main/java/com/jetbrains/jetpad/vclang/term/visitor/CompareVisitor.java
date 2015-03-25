package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

import java.util.ArrayList;
import java.util.List;

public class CompareVisitor implements AbstractExpressionVisitor<Abstract.Expression, Boolean> {
  private final List<Equation> myEquations = new ArrayList<>();

  public static class Equation {
    public Abstract.HoleExpression hole;
    public Abstract.Expression expression;

    public Equation(Abstract.HoleExpression hole, Abstract.Expression expression) {
      this.hole = hole;
      this.expression = expression;
    }
  }

  public List<Equation> equations() {
    return myEquations;
  }

  @Override
  public Boolean visitApp(Abstract.AppExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    if (!(other instanceof Abstract.AppExpression)) return false;
    Abstract.AppExpression otherApp = (Abstract.AppExpression) other;
    return expr.getFunction().accept(this, otherApp.getFunction()) && expr.getArgument().accept(this, otherApp.getArgument());
  }

  @Override
  public Boolean visitDefCall(Abstract.DefCallExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.DefCallExpression && expr.getDefinition().equals(((Abstract.DefCallExpression) other).getDefinition());
  }

  @Override
  public Boolean visitIndex(Abstract.IndexExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.IndexExpression && expr.getIndex() == ((Abstract.IndexExpression) other).getIndex();
  }

  @Override
  public Boolean visitLam(Abstract.LamExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.LamExpression && expr.getBody().accept(this, ((Abstract.LamExpression) other).getBody());
  }

  @Override
  public Boolean visitNat(Abstract.NatExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.NatExpression;
  }

  @Override
  public Boolean visitNelim(Abstract.NelimExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.NelimExpression;
  }

  @Override
  public Boolean visitPi(Abstract.PiExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    if (!(other instanceof Abstract.PiExpression)) return false;
    Abstract.PiExpression otherPi = (Abstract.PiExpression) other;
    return expr.getDomain().accept(this, otherPi.getDomain()) && expr.getCodomain().accept(this, otherPi.getCodomain());
  }

  @Override
  public Boolean visitSuc(Abstract.SucExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.SucExpression;
  }

  @Override
  public Boolean visitUniverse(Abstract.UniverseExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.UniverseExpression && (expr.getLevel() == -1 || expr.getLevel() >= ((Abstract.UniverseExpression) other).getLevel());
  }

  @Override
  public Boolean visitVar(Abstract.VarExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.VarExpression && expr.getName().equals(((Abstract.VarExpression) other).getName());
  }

  @Override
  public Boolean visitZero(Abstract.ZeroExpression expr, Abstract.Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.ZeroExpression;
  }

  @Override
  public Boolean visitHole(Abstract.HoleExpression expr, Abstract.Expression other) {
    if (expr instanceof CheckTypeVisitor.InferHoleExpression) {
      myEquations.add(new Equation(expr, other));
      return true;
    }

    return expr == other;
  }
}
