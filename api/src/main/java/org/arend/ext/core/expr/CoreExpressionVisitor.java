package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public interface CoreExpressionVisitor<P, R> {
  R visitApp(@NotNull CoreAppExpression expr, P params);
  R visitFunCall(@NotNull CoreFunCallExpression expr, P params);
  R visitConCall(@NotNull CoreConCallExpression expr, P params);
  R visitDataCall(@NotNull CoreDataCallExpression expr, P params);
  R visitFieldCall(@NotNull CoreFieldCallExpression expr, P params);
  R visitClassCall(@NotNull CoreClassCallExpression expr, P params);
  R visitReference(@NotNull CoreReferenceExpression expr, P params);
  R visitInferenceReference(@NotNull CoreInferenceReferenceExpression expr, P params);
  R visitLam(@NotNull CoreLamExpression expr, P params);
  R visitPi(@NotNull CorePiExpression expr, P params);
  R visitSigma(@NotNull CoreSigmaExpression expr, P params);
  R visitUniverse(@NotNull CoreUniverseExpression expr, P params);
  R visitError(@NotNull CoreErrorExpression expr, P params);
  R visitTuple(@NotNull CoreTupleExpression expr, P params);
  R visitProj(@NotNull CoreProjExpression expr, P params);
  R visitNew(@NotNull CoreNewExpression expr, P params);
  R visitPEval(@NotNull CorePEvalExpression expr, P params);
  R visitLet(@NotNull CoreLetExpression expr, P params);
  R visitCase(@NotNull CoreCaseExpression expr, P params);
  R visitInteger(@NotNull CoreIntegerExpression expr, P params);
  R visitTypeConstructor(@NotNull CoreTypeConstructorExpression expr, P params);
  R visitTypeDestructor(@NotNull CoreTypeDestructorExpression expr, P params);
  R visitArray(@NotNull CoreArrayExpression expr, P params);
  R visitPath(@NotNull CorePathExpression expr, P params);
  R visitAt(@NotNull CoreAtExpression expr, P params);
}
