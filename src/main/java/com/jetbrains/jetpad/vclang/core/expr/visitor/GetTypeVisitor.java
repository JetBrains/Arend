package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.ArrayList;
import java.util.List;

public class GetTypeVisitor extends BaseExpressionVisitor<Void, Expression> {
  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    Expression functionType = expr.getFunction().accept(this, null);
    if (functionType == null) {
      return null;
    }
    functionType = functionType.normalize(NormalizeVisitor.Mode.WHNF);
    return functionType instanceof ErrorExpression ? functionType : functionType.applyExpressions(expr.getArguments());
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    List<DependentLink> defParams = new ArrayList<>();
    Expression type = expr.getDefinition().getTypeWithParams(defParams, expr.getSortArgument());
    assert expr.getDefCallArguments().size() == defParams.size();
    return type.subst(DependentLink.Helper.toSubstitution(defParams, expr.getDefCallArguments()), expr.getSortArgument().toLevelSubstitution());
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Void params) {
    List<DependentLink> defParams = new ArrayList<>();
    Expression type = expr.getDefinition().getTypeWithParams(defParams, expr.getSortArgument());
    assert expr.getDataTypeArguments().size() + expr.getDefCallArguments().size() == defParams.size();
    ExprSubstitution subst = DependentLink.Helper.toSubstitution(defParams, expr.getDataTypeArguments());
    defParams = defParams.subList(expr.getDataTypeArguments().size(), defParams.size());
    subst.addAll(DependentLink.Helper.toSubstitution(defParams, expr.getDefCallArguments()));
    return type.subst(subst, LevelSubstitution.EMPTY);
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
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : expr.getVariable().getType().getExpr();
  }

  @Override
  public Expression visitLam(LamExpression expr, Void ignored) {
    Expression bodyType = expr.getBody().accept(this, null);
    return bodyType == null ? null : new PiExpression(expr.getPLevel(), expr.getParameters(), bodyType);
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    return new UniverseExpression(new Sort(expr.getPLevel(), expr.getCodomain().accept(this, null).toSort().getHLevel()));
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
    return expr.getType();
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void ignored) {
    Expression type = expr.getExpression().accept(this, null);
    if (type == null) {
      return null;
    }

    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    if (type instanceof ErrorExpression) {
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
      if (!params.hasNext()) {
        return null;
      }
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
    Expression type = expr.getExpression().accept(this, null);
    if (type == null) {
      return null;
    }
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    if (type instanceof ErrorExpression) {
      return type;
    }
    return new LetExpression(expr.getClauses(), type);
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getType();
  }
}
