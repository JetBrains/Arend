package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.model.expr.Model;
import com.jetbrains.jetpad.vclang.term.expr.*;

import java.util.List;

public class ToModelVisitor implements ExpressionVisitor<Model.Expression> {
  private final List<String> myLocalContext;

  public ToModelVisitor(List<String> localContext) {
    myLocalContext = localContext;
  }

  @Override
  public Model.AppExpression visitApp(AppExpression expr) {
    Model.AppExpression result = new Model.AppExpression();
    result.function().set(expr.getFunction().accept(this));
    result.argument().set(expr.getArgument().accept(this));
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.VarExpression visitDefCall(DefCallExpression expr) {
    Model.VarExpression result = new Model.VarExpression();
    result.name().set(expr.getDefinition().getName());
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.VarExpression visitIndex(IndexExpression expr) {
    assert expr.getIndex() < myLocalContext.size();
    Model.VarExpression result = new Model.VarExpression();
    result.name().set(myLocalContext.get(expr.getIndex()));
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.LamExpression visitLam(LamExpression expr) {
    Model.LamExpression result = new Model.LamExpression();
    result.variable().set(expr.getVariable());
    myLocalContext.add(expr.getVariable());
    result.body().set(expr.accept(this));
    myLocalContext.remove(myLocalContext.size() - 1);
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.NatExpression visitNat(NatExpression expr) {
    Model.NatExpression result = new Model.NatExpression();
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.NelimExpression visitNelim(NelimExpression expr) {
    Model.NelimExpression result = new Model.NelimExpression();
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.PiExpression visitPi(PiExpression expr) {
    Model.PiExpression result = new Model.PiExpression();
    Model.Argument arg = new Model.Argument();
    String var = expr.getVariable();
    arg.isExplicit().set(expr.isExplicit());
    arg.name().set(var);
    arg.type().set(expr.getDomain().accept(this));
    result.domain().set(arg);
    if (var != null) {
      myLocalContext.add(expr.getVariable());
    }
    result.codomain().set(expr.getCodomain().accept(this));
    if (var != null) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.SucExpression visitSuc(SucExpression expr) {
    Model.SucExpression result = new Model.SucExpression();
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.UniverseExpression visitUniverse(UniverseExpression expr) {
    Model.UniverseExpression result = new Model.UniverseExpression();
    result.level().set(expr.getLevel());
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.VarExpression visitVar(VarExpression expr) {
    Model.VarExpression result = new Model.VarExpression();
    result.name().set(expr.getName());
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.ZeroExpression visitZero(ZeroExpression expr) {
    Model.ZeroExpression result = new Model.ZeroExpression();
    result.wellTypedExpr().set(expr);
    return result;
  }

  @Override
  public Model.Expression visitError(ErrorExpression expr) {
    // TODO: Check if expr.expression() == null.
    Model.Expression result = expr.expression().accept(this);
    result.wellTypedExpr().set(expr);
    return result;
  }
}
