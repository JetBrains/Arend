package org.arend.term.expr.visitor;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.visitor.CorrespondedSubExprVisitor;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.arend.term.concrete.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CorrespondedSubExpr extends TypeCheckingTestCase {
  @Test
  public void testMultiParamLam() {
    Concrete.LamExpression xyx = (Concrete.LamExpression) resolveNamesExpr("\\lam x y => x");
    Concrete.UniverseExpression ty = cUniverseStd(0);
    Concrete.TypeParameter t = cTypeArg(ty);
    Expression pi = typeCheckExpr(cPi(Arrays.asList(t, t), ty), null).expression;
    Expression accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getBody()), typeCheckExpr(xyx, pi).expression);
    ReferenceExpression referenceExpression = accept.cast(ReferenceExpression.class);
    assertNotNull(referenceExpression);
    assertEquals(referenceExpression.getBinding().getName(), "x");
  }

  @Test
  public void testSimpleLam() {
    Concrete.LamExpression xx = (Concrete.LamExpression) resolveNamesExpr("\\lam x => x");
    Concrete.UniverseExpression ty = cUniverseStd(0);
    Expression pi = typeCheckExpr(cPi(Collections.singletonList(cTypeArg(ty)), ty), null).expression;
    Expression accept = xx.accept(new CorrespondedSubExprVisitor(xx.getBody()), typeCheckExpr(xx, pi).expression);
    ReferenceExpression referenceExpression = accept.cast(ReferenceExpression.class);
    assertNotNull(referenceExpression);
    assertEquals(referenceExpression.getBinding().getName(), "x");
  }
}
