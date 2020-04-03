package org.arend.typechecking;

import org.arend.core.expr.Expression;
import org.arend.core.expr.LamExpression;
import org.arend.core.expr.TupleExpression;
import org.arend.term.concrete.Concrete;
import org.junit.Test;

import static org.junit.Assert.*;

public class AppHoleTest extends TypeCheckingTestCase {
  @Test
  public void inBinOp() {
    Expression ty = typeCheckExpr("Nat -> Nat", null).expression;
    assertTrue(typeCheckExpr("__ Nat.+ 114514", ty)
        .expression instanceof LamExpression);
  }

  @Test
  public void inNestedBinOpWithParen() {
    Expression ty = typeCheckExpr("Nat -> Nat", null).expression;
    assertTrue(typeCheckExpr("114 Nat.* (__ Nat.+ 514)", ty)
        .expression instanceof LamExpression);
  }

  @Test
  public void inNestedBinOp() {
    Expression ty = typeCheckExpr("Nat -> Nat", null).expression;
    assertTrue(typeCheckExpr("114 Nat.* __ Nat.+ 514", ty)
        .expression instanceof LamExpression);
  }

  @Test
  public void inAppInOp() {
    Expression ty = typeCheckExpr("Nat -> Nat", null).expression;
    assertTrue(typeCheckExpr("suc __ Nat.+ 666", ty)
        .expression instanceof LamExpression);
  }

  @Test
  public void inNestedAppInOp() {
    Concrete.Expression expression = resolveNamesExpr("suc (suc __) Nat.+ 233");
    assertNotNull(expression);
    Concrete.Expression sucSuc__ = ((Concrete.AppExpression) expression).getArguments().get(0).expression;
    assertTrue(sucSuc__ instanceof Concrete.AppExpression);
    typeCheckExpr(expression, null, 1);
  }

  @Test
  public void inLam() {
    Expression ty = typeCheckExpr("Nat -> Nat -> Nat", null).expression;
    assertTrue(typeCheckExpr("\\lam x => __ Nat.+ x", ty)
        .expression instanceof LamExpression);
  }

  @Test
  public void inCase() {
    Expression ty = typeCheckExpr("Nat -> Nat -> Nat", null).expression;
    assertTrue(typeCheckExpr(
        "\\case __, 666 Nat.+ __ \\return Nat \\with {\n" +
            "  | _, _ => 1\n" +
            "}", ty).expression instanceof LamExpression);
  }

  @Test
  public void inBinOpWithProj() {
    Expression ty = typeCheckExpr("(\\Sigma Nat Nat) -> Nat", null).expression;
    assertTrue(typeCheckExpr("__.1 Nat.+ 233", ty)
        .expression instanceof LamExpression);
  }

  @Test
  public void inTuple() {
    Expression ty = typeCheckExpr("(\\Sigma ((\\Sigma Nat Nat) -> Nat) ((\\Sigma Nat Nat) -> Nat))", null).expression;
    Expression result = typeCheckExpr("(__.1, __.2)", ty).expression;
    assertTrue(result instanceof TupleExpression);
    TupleExpression tuple = (TupleExpression) result;
    assertEquals(2, tuple.getFields().size());
    assertTrue(tuple.getFields().get(0) instanceof LamExpression);
    assertTrue(tuple.getFields().get(1) instanceof LamExpression);
  }

  @Test
  public void inApplicant() {
    Expression ty = typeCheckExpr("(Nat -> Nat) -> Nat", null).expression;
    assertTrue(typeCheckExpr("__ 233", ty)
        .expression instanceof LamExpression);
  }

  @Test
  public void implicit() {
    assertTrue(typeCheckExpr("idp {__}", null)
        .expression instanceof LamExpression);
  }

  @Test
  public void inApp() {
    Expression ty = typeCheckExpr("\\Set0 -> \\Set1", null).expression;
    assertTrue(typeCheckExpr("Path (\\lam _ => \\Set0) __ Nat", ty)
        .expression instanceof LamExpression);
  }

  @Test
  public void inProj() {
    typeCheckDef("\\func test : (\\Sigma Nat Nat) -> Nat => __.1");
    typeCheckDef("\\func test : (\\Sigma Nat Nat) -> Nat => __.2");
    typeCheckDef("\\func test : (\\Sigma Nat) -> Nat => __.1");
  }
}
