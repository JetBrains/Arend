package org.arend.term.expr.visitor;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.visitor.CorrespondedSubExprVisitor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CorrespondedSubExpr extends TypeCheckingTestCase {
  @Test
  public void multiParamLam() {
    Concrete.LamExpression xyx = (Concrete.LamExpression) resolveNamesExpr("\\lam x y => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x y : \\Type) -> \\Type"), null).expression;
    Expression accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getBody()), typeCheckExpr(xyx, pi).expression);
    ReferenceExpression referenceExpression = accept.cast(ReferenceExpression.class);
    assertNotNull(referenceExpression);
    assertEquals(referenceExpression.getBinding().getName(), "x");
  }

  @Test
  public void simpleLam() {
    Concrete.LamExpression xx = (Concrete.LamExpression) resolveNamesExpr("\\lam x => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x : \\Type) -> \\Type"), null).expression;
    Expression accept = xx.accept(new CorrespondedSubExprVisitor(xx.getBody()), typeCheckExpr(xx, pi).expression);
    ReferenceExpression referenceExpression = accept.cast(ReferenceExpression.class);
    assertNotNull(referenceExpression);
    assertEquals(referenceExpression.getBinding().getName(), "x");
  }

  @Test
  public void simplePi() {
    Concrete.PiExpression xyx = (Concrete.PiExpression) resolveNamesExpr("\\Pi (A : \\Type) (x y : A) -> A");
    Expression pi = typeCheckExpr(xyx, null).expression;
    Expression accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(1).getType()), pi);
    ReferenceExpression referenceExpression = accept.cast(ReferenceExpression.class);
    assertNotNull(referenceExpression);
    assertEquals(referenceExpression.getBinding().getName(), "A");
  }

  @Test
  public void complexPi() {
    Concrete.PiExpression xyx = (Concrete.PiExpression) resolveNamesExpr("\\Pi (A B : \\Type) (x y : A) -> B");
    Expression pi = typeCheckExpr(xyx, null).expression;
    Expression accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(1).getType()), pi);
    ReferenceExpression referenceExpression = accept.cast(ReferenceExpression.class);
    assertNotNull(referenceExpression);
    assertEquals(referenceExpression.getBinding().getName(), "A");
  }

  @Test
  public void telescopeSigma() {

  }
}
