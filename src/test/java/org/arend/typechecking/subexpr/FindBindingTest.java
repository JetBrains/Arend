package org.arend.typechecking.subexpr;

import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FindBindingTest extends TypeCheckingTestCase {
  @SuppressWarnings("unchecked")
  private static <T> T c(Object o) {
    return (T) o;
  }

  @Test
  public void multiParamLam() {
    Concrete.LamExpression xyx = (Concrete.LamExpression) resolveNamesExpr("\\lam x y => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x y : \\Type) -> \\Type"), null).expression;
    Expression lam = FindBinding.visitLam(
        xyx.getParameters().get(1).getReferableList().get(0),
        c(xyx), c(typeCheckExpr(xyx, pi).expression)
    ).getTypeExpr();
    assertEquals("\\Type", lam.toString());
  }

  @Test
  public void simpleLam() {
    Concrete.LamExpression xx = (Concrete.LamExpression) resolveNamesExpr("\\lam x => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x : \\Type) -> \\Type"), null).expression;
    Expression lam = FindBinding.visitLam(
        xx.getParameters().get(0).getReferableList().get(0),
        c(xx), c(typeCheckExpr(xx, pi).expression)
    ).getTypeExpr();
    assertEquals("\\Type", lam.toString());
  }

  @Test
  public void pi() {
    Concrete.PiExpression xyx = (Concrete.PiExpression) resolveNamesExpr("\\Pi (A B : \\Type) (x y : A) -> B");
    Expression pi = FindBinding.visitPi(
        xyx.getParameters().get(1).getReferableList().get(1),
        xyx, c(typeCheckExpr(xyx, null).expression)
    ).getTypeExpr();
    assertEquals("A", pi.toString());
  }

  @Test
  public void let() {
    Concrete.LetExpression xyx = (Concrete.LetExpression) resolveNamesExpr("\\let | a => 1 \\in a");
    Expression let = FindBinding.visitLet(
        xyx.getClauses().get(0).getPattern().getData(),
        xyx, c(typeCheckExpr(xyx, null).expression));
    assertNotNull(let);
    assertEquals("Nat", let.toString());
  }

  @Test
  public void sigma() {
    Concrete.SigmaExpression xyx = (Concrete.SigmaExpression) resolveNamesExpr("\\Sigma (A B : \\Type) (x y : A) A");
    Expression sig = typeCheckExpr(xyx, null).expression;
    {
      Expression accept = FindBinding.visitSigma(
          xyx.getParameters().get(0).getReferableList().get(0),
          xyx, c(sig)
      ).getTypeExpr();
      assertEquals("\\Type", accept.toString());
    }
    {
      Expression accept = FindBinding.visitSigma(
          xyx.getParameters().get(1).getReferableList().get(0),
          xyx, c(sig)
      ).getTypeExpr();
      assertEquals("A", accept.toString());
    }
  }
}