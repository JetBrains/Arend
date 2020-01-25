package org.arend.term.expr.visitor;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.visitor.CorrespondedSubExprVisitor;
import org.arend.util.Pair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CorrespondedSubExprTest extends TypeCheckingTestCase {
  @Test
  public void multiParamLam() {
    Concrete.LamExpression xyx = (Concrete.LamExpression) resolveNamesExpr("\\lam x y => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x y : \\Type) -> \\Type"), null).expression;
    Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getBody()), typeCheckExpr(xyx, pi).expression);
    assertEquals(accept.proj1.cast(ReferenceExpression.class).getBinding().getName(), "x");
  }

  @Test
  public void simpleLam() {
    Concrete.LamExpression xx = (Concrete.LamExpression) resolveNamesExpr("\\lam x => x");
    Expression pi = typeCheckExpr(resolveNamesExpr("\\Pi (x : \\Type) -> \\Type"), null).expression;
    Pair<Expression, Concrete.Expression> accept = xx.accept(new CorrespondedSubExprVisitor(xx.getBody()), typeCheckExpr(xx, pi).expression);
    assertEquals(accept.proj1.cast(ReferenceExpression.class).getBinding().getName(), "x");
  }

  @Test
  public void simplePi() {
    Concrete.PiExpression xyx = (Concrete.PiExpression) resolveNamesExpr("\\Pi (A : \\Type) (x y : A) -> A");
    Expression pi = typeCheckExpr(xyx, null).expression;
    Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(1).getType()), pi);
    assertEquals(accept.proj1.cast(ReferenceExpression.class).getBinding().getName(), "A");
  }

  @Test
  public void complexPi() {
    Concrete.PiExpression xyx = (Concrete.PiExpression) resolveNamesExpr("\\Pi (A B : \\Type) (x y : A) -> B");
    Expression pi = typeCheckExpr(xyx, null).expression;
    Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(1).getType()), pi);
    assertEquals(accept.proj1.cast(ReferenceExpression.class).getBinding().getName(), "A");
  }

  @Test
  public void complexSigma() {
    Concrete.SigmaExpression xyx = (Concrete.SigmaExpression) resolveNamesExpr("\\Sigma (A B : \\Type) (x y : A) A");
    Expression sig = typeCheckExpr(xyx, null).expression;
    {
      Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(1).getType()), sig);
      assertEquals(accept.proj1.cast(ReferenceExpression.class).getBinding().getName(), "A");
    }
    {
      Pair<Expression, Concrete.Expression> accept = xyx.accept(new CorrespondedSubExprVisitor(xyx.getParameters().get(2).getType()), sig);
      assertEquals(accept.proj1.cast(ReferenceExpression.class).getBinding().getName(), "A");
    }
  }

  @Test
  public void simpleDefCall() {
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
      assertEquals(accept.proj1.toString(), "f b a");
    }
  }
}
