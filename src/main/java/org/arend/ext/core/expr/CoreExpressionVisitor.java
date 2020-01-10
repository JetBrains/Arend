package org.arend.ext.core.expr;

import javax.annotation.Nonnull;

public interface CoreExpressionVisitor<P, R> {
  R visitApp(@Nonnull CoreAppExpression expr, P params);
  R visitFunCall(@Nonnull CoreFunCallExpression expr, P params);
  R visitConCall(@Nonnull CoreConCallExpression expr, P params);
  R visitDataCall(@Nonnull CoreDataCallExpression expr, P params);
  R visitFieldCall(@Nonnull CoreFieldCallExpression expr, P params);
  R visitClassCall(@Nonnull CoreClassCallExpression expr, P params);
  R visitReference(@Nonnull CoreReferenceExpression expr, P params);
  R visitInferenceReference(@Nonnull CoreInferenceReferenceExpression expr, P params);
  R visitLam(@Nonnull CoreLamExpression expr, P params);
  R visitPi(@Nonnull CorePiExpression expr, P params);
  R visitSigma(@Nonnull CoreSigmaExpression expr, P params);
  R visitUniverse(@Nonnull CoreUniverseExpression expr, P params);
  R visitError(@Nonnull CoreErrorExpression expr, P params);
  R visitTuple(@Nonnull CoreTupleExpression expr, P params);
  R visitProj(@Nonnull CoreProjExpression expr, P params);
  R visitNew(@Nonnull CoreNewExpression expr, P params);
  R visitPEval(@Nonnull CorePEvalExpression expr, P params);
  R visitLet(@Nonnull CoreLetExpression expr, P params);
  R visitCase(@Nonnull CoreCaseExpression expr, P params);
  R visitInteger(@Nonnull CoreIntegerExpression expr, P params);
}
