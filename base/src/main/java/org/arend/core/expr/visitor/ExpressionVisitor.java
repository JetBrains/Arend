package org.arend.core.expr.visitor;

import org.arend.core.expr.*;

public interface ExpressionVisitor<P, R> {
  R visitApp(AppExpression expr, P params);
  R visitFunCall(FunCallExpression expr, P params);
  R visitConCall(ConCallExpression expr, P params);
  R visitDataCall(DataCallExpression expr, P params);
  R visitFieldCall(FieldCallExpression expr, P params);
  R visitClassCall(ClassCallExpression expr, P params);
  R visitReference(ReferenceExpression expr, P params);
  R visitInferenceReference(InferenceReferenceExpression expr, P params);
  R visitSubst(SubstExpression expr, P params);
  R visitLam(LamExpression expr, P params);
  R visitPi(PiExpression expr, P params);
  R visitSigma(SigmaExpression expr, P params);
  R visitUniverse(UniverseExpression expr, P params);
  R visitError(ErrorExpression expr, P params);
  R visitTuple(TupleExpression expr, P params);
  R visitProj(ProjExpression expr, P params);
  R visitNew(NewExpression expr, P params);
  R visitPEval(PEvalExpression expr, P params);
  R visitBox(BoxExpression expr, P params);
  R visitLet(LetExpression expr, P params);
  R visitCase(CaseExpression expr, P params);
  R visitOfType(OfTypeExpression expr, P params);
  R visitInteger(IntegerExpression expr, P params);
  R visitTypeConstructor(TypeConstructorExpression expr, P params);
  R visitTypeDestructor(TypeDestructorExpression expr, P params);
  R visitArray(ArrayExpression expr, P params);
  R visitPath(PathExpression expr, P params);
  R visitAt(AtExpression expr, P params);
}
