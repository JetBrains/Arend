package org.arend.core.expr.visitor;

import org.arend.core.expr.*;

public interface ExpressionVisitor2<P1,P2,R> {
  R visitApp(AppExpression expr, P1 param1, P2 param2);
  R visitFunCall(FunCallExpression expr, P1 param1, P2 param2);
  R visitConCall(ConCallExpression expr, P1 param1, P2 param2);
  R visitDataCall(DataCallExpression expr, P1 param1, P2 param2);
  R visitFieldCall(FieldCallExpression expr, P1 param1, P2 param2);
  R visitClassCall(ClassCallExpression expr, P1 param1, P2 param2);
  R visitReference(ReferenceExpression expr, P1 param1, P2 param2);
  R visitInferenceReference(InferenceReferenceExpression expr, P1 param1, P2 param2);
  R visitSubst(SubstExpression expr, P1 param1, P2 param2);
  R visitLam(LamExpression expr, P1 param1, P2 param2);
  R visitPi(PiExpression expr, P1 param1, P2 param2);
  R visitSigma(SigmaExpression expr, P1 param1, P2 param2);
  R visitUniverse(UniverseExpression expr, P1 param1, P2 param2);
  R visitError(ErrorExpression expr, P1 param1, P2 param2);
  R visitTuple(TupleExpression expr, P1 param1, P2 param2);
  R visitProj(ProjExpression expr, P1 param1, P2 param2);
  R visitNew(NewExpression expr, P1 param1, P2 param2);
  R visitPEval(PEvalExpression expr, P1 param1, P2 param2);
  R visitLet(LetExpression expr, P1 param1, P2 param2);
  R visitCase(CaseExpression expr, P1 param1, P2 param2);
  R visitOfType(OfTypeExpression expr, P1 param1, P2 param2);
  R visitInteger(IntegerExpression expr, P1 param1, P2 param2);
}
