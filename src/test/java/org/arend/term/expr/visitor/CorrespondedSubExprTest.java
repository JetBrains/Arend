package org.arend.term.expr.visitor;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.prelude.PreludeLibrary;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.visitor.CorrespondedSubExprVisitor;
import org.arend.util.Pair;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CorrespondedSubExprTest extends TypeCheckingTestCase {
  @Test
  public void multiParamLam() {
    Concrete.LamExpression xyx = (Concrete.LamExpression) resolveNamesExpr("\\lam x y => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x y : \\Type) -> \\Type"), null).expression;
    Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getBody()), typeCheckExpr(xyx, pi).expression);
    assertEquals("x", accept.proj1.cast(ReferenceExpression.class).getBinding().getName());
  }

  @Test
  public void simpleLam() {
    Concrete.LamExpression xx = (Concrete.LamExpression) resolveNamesExpr("\\lam x => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x : \\Type) -> \\Type"), null).expression;
    Pair<Expression, Concrete.Expression> accept = xx.accept(new CorrespondedSubExprVisitor(xx.getBody()), typeCheckExpr(xx, pi).expression);
    assertEquals("x", accept.proj1.cast(ReferenceExpression.class).getBinding().getName());
  }

  @Test
  public void simplePi() {
    Concrete.PiExpression xyx = (Concrete.PiExpression) resolveNamesExpr("\\Pi (A : \\Type) (x y : A) -> A");
    Expression pi = typeCheckExpr(xyx, null).expression;
    Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(1).getType()), pi);
    assertEquals("A", accept.proj1.cast(ReferenceExpression.class).getBinding().getName());
  }

  @Test
  public void complexPi() {
    Concrete.PiExpression xyx = (Concrete.PiExpression) resolveNamesExpr("\\Pi (A B : \\Type) (x y : A) -> B");
    Expression pi = typeCheckExpr(xyx, null).expression;
    Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(1).getType()), pi);
    assertEquals("A", accept.proj1.cast(ReferenceExpression.class).getBinding().getName());
  }

  @Test
  public void complexSigma() {
    Concrete.SigmaExpression xyx = (Concrete.SigmaExpression) resolveNamesExpr("\\Sigma (A B : \\Type) (x y : A) A");
    Expression sig = typeCheckExpr(xyx, null).expression;
    {
      Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(1).getType()), sig);
      assertEquals("A", accept.proj1.cast(ReferenceExpression.class).getBinding().getName());
    }
    {
      Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(2).getType()), sig);
      assertEquals("A", accept.proj1.cast(ReferenceExpression.class).getBinding().getName());
    }
  }

  @Test
  public void simpleAppExpr() {
    Concrete.LamExpression expr = (Concrete.LamExpression) resolveNamesExpr("\\lam {A : \\Type} (f : A -> A -> A) (a b : A) => f b a");
    Expression core = typeCheckExpr(expr, null).expression;
    Concrete.AppExpression body = (Concrete.AppExpression) expr.getBody();
    {
      Pair<Expression, Concrete.Expression> accept = expr.accept(new CorrespondedSubExprVisitor(body.getArguments().get(0).getExpression()), core);
      assertEquals(accept.proj1.cast(ReferenceExpression.class).getBinding().getName(), "b");
    }
    {
      Pair<Expression, Concrete.Expression> accept = expr.accept(new CorrespondedSubExprVisitor(body.getArguments().get(1).getExpression()), core);
      assertEquals(accept.proj1.cast(ReferenceExpression.class).getBinding().getName(), "a");
    }
    {
      Concrete.Expression make = Concrete.AppExpression.make(body.getData(), body.getFunction(), body.getArguments().get(0).getExpression(), true);
      Pair<Expression, Concrete.Expression> accept = expr.accept(new CorrespondedSubExprVisitor(make), core);
      // Selects the whole expression
      assertEquals("f b a", accept.proj1.toString());
    }
  }

  @Test
  public void infixDefCall() {
    // (1+3*2)-4
    Concrete.AppExpression expr = (Concrete.AppExpression) resolveNamesExpr(
        PreludeLibrary.getPreludeScope(),
        "1 Nat.+ 3 Nat.* 2 Nat.- 4");
    Expression core = typeCheckExpr(expr, null).expression;
    Concrete.Expression sub = expr.getFunction();
    Concrete.Argument four = expr.getArguments().get(1);
    // 1+3*2
    Concrete.Argument arg1 = expr.getArguments().get(0);
    // 1+3*2
    Concrete.AppExpression expr2 = (Concrete.AppExpression) arg1.getExpression();
    Concrete.Expression add = expr2.getFunction();
    Concrete.Argument one = expr2.getArguments().get(0);
    // 3*2
    Concrete.Argument arg2 = expr2.getArguments().get(1);
    Concrete.AppExpression expr3 = (Concrete.AppExpression) arg2.getExpression();
    Concrete.Expression mul = expr3.getFunction();
    Concrete.Argument three = expr3.getArguments().get(0);
    Concrete.Argument two = expr3.getArguments().get(1);
    {
      // 3* -> 3*2
      Concrete.Expression make = Concrete.AppExpression.make(expr3.getData(), mul, three.getExpression(), true);
      Pair<Expression, Concrete.Expression> accept = expr.accept(new CorrespondedSubExprVisitor(make), core);
      assertEquals("3 * 2", accept.proj1.toString());
      assertEquals("3 * 2", accept.proj2.toString());
    }
    {
      // *2 -> 3*2
      Concrete.Expression make = Concrete.AppExpression.make(expr3.getData(), mul, two.getExpression(), true);
      Pair<Expression, Concrete.Expression> accept = expr.accept(new CorrespondedSubExprVisitor(make), core);
      assertEquals("3 * 2", accept.proj1.toString());
      assertEquals("3 * 2", accept.proj2.toString());
    }
    {
      // 2 -> 2
      Pair<Expression, Concrete.Expression> accept = expr.accept(new CorrespondedSubExprVisitor(two.getExpression()), core);
      assertEquals("2", accept.proj1.toString());
      assertEquals("2", accept.proj2.toString());
    }
    {
      // 3*2 -> 3*2
      Pair<Expression, Concrete.Expression> accept = expr.accept(new CorrespondedSubExprVisitor(expr3), core);
      assertEquals("3 * 2", accept.proj1.toString());
      assertEquals("3 * 2", accept.proj2.toString());
    }
    {
      // 1+ -> 1+3*2
      Concrete.Expression make = Concrete.AppExpression.make(expr3.getData(), add, one.getExpression(), true);
      Pair<Expression, Concrete.Expression> accept = expr.accept(new CorrespondedSubExprVisitor(make), core);
      assertEquals("1 + 3 * 2", accept.proj1.toString());
      assertEquals("1 + 3 * 2", accept.proj2.toString());
    }
  }
}
