package org.arend.typechecking.subexpr;

import org.arend.core.context.param.DependentLink;
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
    DependentLink link = FindBinding.visitLam(
        xyx.getParameters().get(1).getReferableList().get(0),
        c(xyx), c(typeCheckExpr(xyx, pi).expression)
    );
    assertNotNull(link);
    assertEquals("\\Type", link.getTypeExpr().toString());
  }

  @Test
  public void simpleLam() {
    Concrete.LamExpression xx = (Concrete.LamExpression) resolveNamesExpr("\\lam x => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x : \\Type) -> \\Type"), null).expression;
    DependentLink link = FindBinding.visitLam(
        xx.getParameters().get(0).getReferableList().get(0),
        c(xx), c(typeCheckExpr(xx, pi).expression)
    );
    assertNotNull(link);
    assertEquals("\\Type", link.getTypeExpr().toString());
  }

  @Test
  public void pi() {
    Concrete.PiExpression xyx = (Concrete.PiExpression) resolveNamesExpr("\\Pi (A B : \\Type) (x y : A) -> B");
    DependentLink link = FindBinding.visitPi(
        xyx.getParameters().get(1).getReferableList().get(1),
        xyx, c(typeCheckExpr(xyx, null).expression)
    );
    assertNotNull(link);
    assertEquals("A", link.getTypeExpr().toString());
  }

  @Test
  public void let() {
    Concrete.LetExpression xyx = (Concrete.LetExpression) resolveNamesExpr("\\let | a => 114514 \\in a");
    Expression let = FindBinding.visitLet(
        xyx.getClauses().get(0).getPattern().getData(),
        xyx, c(typeCheckExpr(xyx, null).expression));
    assertNotNull(let);
    assertEquals("Nat", let.toString());
  }

  @Test
  public void letParam() {
    Concrete.LetExpression xyx = (Concrete.LetExpression) resolveNamesExpr("\\let | f a => Nat.suc a \\in f 114514");
     DependentLink let = FindBinding.visitLetParam(
        xyx.getClauses().get(0).getParameters().get(0).getReferableList().get(0),
        xyx, c(typeCheckExpr(xyx, null).expression));
    assertNotNull(let);
    assertEquals("Nat", let.getTypeExpr().toString());
  }

  @Test
  public void sigma() {
    Concrete.SigmaExpression xyx = (Concrete.SigmaExpression) resolveNamesExpr("\\Sigma (A B : \\Type) (x y : A) A");
    Expression sig = typeCheckExpr(xyx, null).expression;
    {
      DependentLink link = FindBinding.visitSigma(
          xyx.getParameters().get(0).getReferableList().get(0),
          xyx, c(sig)
      );
      assertNotNull(link);
      assertEquals("\\Type", link.getTypeExpr().toString());
    }
    {
      DependentLink link = FindBinding.visitSigma(
          xyx.getParameters().get(1).getReferableList().get(0),
          xyx, c(sig)
      );
      assertNotNull(link);
      assertEquals("A", link.getTypeExpr().toString());
    }
  }
}