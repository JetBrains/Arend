package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public abstract class BaseCoreExpressionVisitor<P, R> implements CoreExpressionVisitor<P, R> {
  abstract protected R visit(CoreExpression expression, P param);

  @Override
  public R visitApp(@NotNull CoreAppExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitFunCall(@NotNull CoreFunCallExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitConCall(@NotNull CoreConCallExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitDataCall(@NotNull CoreDataCallExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitFieldCall(@NotNull CoreFieldCallExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitClassCall(@NotNull CoreClassCallExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitReference(@NotNull CoreReferenceExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitInferenceReference(@NotNull CoreInferenceReferenceExpression expr, P params) {
    return expr.getSubstExpression() == null ? visit(expr, params) : expr.getSubstExpression().accept(this, params);
  }

  @Override
  public R visitLam(@NotNull CoreLamExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitPi(@NotNull CorePiExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitSigma(@NotNull CoreSigmaExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitUniverse(@NotNull CoreUniverseExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitError(@NotNull CoreErrorExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitTuple(@NotNull CoreTupleExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitProj(@NotNull CoreProjExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitNew(@NotNull CoreNewExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitPEval(@NotNull CorePEvalExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitLet(@NotNull CoreLetExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitCase(@NotNull CoreCaseExpression expr, P params) {
    return visit(expr, params);
  }

  @Override
  public R visitInteger(@NotNull CoreIntegerExpression expr, P params) {
    return visit(expr, params);
  }
}
