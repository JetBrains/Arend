package org.arend.typechecking;

import org.arend.core.expr.Expression;
import org.arend.core.expr.LamExpression;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AppHoleTest extends TypeCheckingTestCase {
  @Test
  public void inBinOp() {
    Expression ty = typeCheckExpr("Nat -> Nat", null).expression;
    Expression result = typeCheckExpr("__ Nat.+ 114514", ty).expression;
    assertTrue(result instanceof LamExpression);
  }

  @Test
  public void inLam() {
    Expression ty = typeCheckExpr("Nat -> Nat -> Nat", null).expression;
    Expression result = typeCheckExpr("\\lam x => __ Nat.+ x", ty).expression;
    assertTrue(result instanceof LamExpression);
  }

  @Test
  public void inApplicant() {
    Expression ty = typeCheckExpr("(Nat -> Nat) -> Nat", null).expression;
    Expression result = typeCheckExpr("__ 233", ty).expression;
    assertTrue(result instanceof LamExpression);
  }

  @Test
  public void inApp() {
    Expression ty = typeCheckExpr("\\Set0 -> \\Set1", null).expression;
    Expression result = typeCheckExpr("Path (\\lam _ => \\Set0) __ Nat", ty).expression;
    assertTrue(result instanceof LamExpression);
  }

  @Test
  public void inProj() {
    typeCheckDef("\\func test : (\\Sigma Nat Nat) -> Nat => __.1");
    typeCheckDef("\\func test : (\\Sigma Nat Nat) -> Nat => __.2");
    typeCheckDef("\\func test : (\\Sigma Nat) -> Nat => __.1");
  }
}
