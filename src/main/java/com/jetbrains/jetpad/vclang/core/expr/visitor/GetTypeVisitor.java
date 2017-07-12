package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.StdLevelSubstitution;

import java.util.ArrayList;
import java.util.List;

public class GetTypeVisitor extends BaseExpressionVisitor<Void, Expression> {
  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    return expr.getFunction().accept(this, null).applyExpression(expr.getArgument());
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    List<DependentLink> defParams = new ArrayList<>();
    Expression type = expr.getDefinition().getTypeWithParams(defParams, expr.getSortArgument());
    assert expr.getDefCallArguments().size() == defParams.size();
    return type.subst(DependentLink.Helper.toSubstitution(defParams, expr.getDefCallArguments()), expr.getSortArgument().toLevelSubstitution());
  }

  @Override
  public UniverseExpression visitDataCall(DataCallExpression expr, Void params) {
    return new UniverseExpression(expr.getDefinition().getSort().subst(new StdLevelSubstitution(expr.getSortArgument())));
  }

  @Override
  public DataCallExpression visitConCall(ConCallExpression expr, Void params) {
    return expr.getDefinition().getDataTypeExpression(expr.getSortArgument(), expr.getDataTypeArguments());
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Void params) {
    return new UniverseExpression(expr.getSort().subst(expr.getSortArgument().toLevelSubstitution()));
  }

  @Override
  public Expression visitLetClauseCall(LetClauseCallExpression expr, Void params) {
    List<DependentLink> defParams = new ArrayList<>();
    Expression type = expr.getLetClause().getTypeWithParams(defParams, null);
    assert expr.getDefCallArguments().size() == defParams.size();
    return type.subst(DependentLink.Helper.toSubstitution(defParams, expr.getDefCallArguments()));
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getType().getExpr().copy();
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : expr.getVariable().getType();
  }

  @Override
  public Expression visitLam(LamExpression expr, Void ignored) {
    return new PiExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, null));
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    return new UniverseExpression(expr.getResultSort());
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    return new UniverseExpression(expr.getSort());
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return new UniverseExpression(expr.getSort().succ());
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    Expression expr1 = null;
    if (expr.getExpr() != null) {
      expr1 = expr.getExpr().accept(this, null);
    }
    return new ErrorExpression(expr1, expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    return expr.getSigmaType();
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void ignored) {
    Expression type = expr.getExpression().accept(this, null).normalize(NormalizeVisitor.Mode.WHNF);
    if (type.toError() != null) {
      return type;
    }

    SigmaExpression sigma = type.toSigma();
    if (sigma == null) return null;
    DependentLink params = sigma.getParameters();
    if (expr.getField() == 0) {
      return params.getType().getExpr();
    }

    ExprSubstitution subst = new ExprSubstitution();
    for (int i = 0; i < expr.getField(); i++) {
      subst.add(params, new ProjExpression(expr.getExpression(), i));
      params = params.getNext();
    }
    return params.getType().subst(subst, LevelSubstitution.EMPTY).getExpr();
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return expr.getExpression();
  }

  @Override
  public Expression visitLet(LetExpression expr, Void params) {
    Expression type = expr.getExpression().accept(this, null).normalize(NormalizeVisitor.Mode.WHNF);
    if (type.toError() != null) {
      return type;
    }
    return new LetExpression(expr.getClauses(), type);
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    return expr.getResultType().subst(DependentLink.Helper.toSubstitution(expr.getParameters(), expr.getArguments()));
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getTypeOf();
  }
}
