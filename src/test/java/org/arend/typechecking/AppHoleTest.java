package org.arend.typechecking;

import org.arend.core.expr.Expression;
import org.arend.core.expr.LamExpression;
import org.arend.core.expr.TupleExpression;
import org.arend.term.concrete.Concrete;
import org.arend.util.ArendExpr;
import org.junit.Test;

import static org.junit.Assert.*;

public class AppHoleTest extends TypeCheckingTestCase {
  private void checkAsLam(@ArendExpr String signature, @ArendExpr String code) {
    Expression ty = typeCheckExpr(signature, null).expression;
    assertTrue(typeCheckExpr(code, ty).expression instanceof LamExpression);
  }

  @Test
  public void inBinOp() {
    checkAsLam("Nat -> Nat", "__ Nat.+ 114514");
  }

  @Test
  public void inBinOp2() {
    checkAsLam("Nat -> Nat", "113 Nat.`mod` __ Nat.`div` 114514");
  }

  @Test
  public void inNestedBinOpWithParen() {
    checkAsLam("Nat -> Nat", "114 Nat.* (__ Nat.+ 514)");
  }

  @Test
  public void inNestedBinOpWithParen2() {
    typeCheckModule(
      "\\func \\infixl 7 % (x : Nat) (f : Nat -> Nat) => f x" +
      "\\func test => % 114 (__ Nat.+ 514)");
  }

  @Test
  public void inNestedBinOp() {
    checkAsLam("Nat -> Nat", "114 Nat.* __ Nat.+ 514");
  }

  @Test
  public void inAppInOp() {
    checkAsLam("Nat -> Nat", "suc __ Nat.+ 666");
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
    checkAsLam("Nat -> Nat -> Nat", "\\lam x => __ Nat.+ x");
  }

  @Test
  public void inPi() {
    checkAsLam("Nat -> \\Set0", "\\Pi (A : 114514 = __) -> Nat");
    checkAsLam("Nat -> \\Set0", "\\Pi (A : 114 = 514 Nat.+ __) -> Nat");
    checkAsLam("\\Set0 -> \\Set1", "\\Pi (A : __) -> Nat");
  }

  @Test
  public void inSigma() {
    checkAsLam("Nat -> \\Set0", "\\Sigma (A : 114514 = __) Nat");
    checkAsLam("Nat -> \\Set0", "\\Sigma (A : 114 = 514 Nat.+ __) Nat");
    checkAsLam("\\Set0 -> \\Set1", "\\Sigma (A : __) Nat");
  }

  @Test
  public void inCase() {
    checkAsLam("Nat -> Nat -> Nat",
        "\\case __, 666 Nat.+ __ \\return Nat \\with {\n" +
            "  | _, _ => 1\n" +
            "}");
  }

  @Test
  public void inBinOpWithProj() {
    checkAsLam("(\\Sigma Nat Nat) -> Nat", "__.1 Nat.+ 233");
  }

  @Test
  public void inTuple() {
    Expression ty = typeCheckExpr(
        "(\\Sigma ((\\Sigma Nat Nat) -> Nat) ((\\Sigma Nat Nat) -> Nat))", null).expression;
    Expression result = typeCheckExpr("(__.1, __.2)", ty).expression;
    assertTrue(result instanceof TupleExpression);
    TupleExpression tuple = (TupleExpression) result;
    assertEquals(2, tuple.getFields().size());
    for (Expression field : tuple.getFields())
      assertTrue(field instanceof LamExpression);
  }

  @Test
  public void inApplicant() {
    checkAsLam("(Nat -> Nat) -> Nat", "__ 233");
  }

  @Test
  public void implicit() {
    assertTrue(typeCheckExpr("idp {__}", null)
        .expression instanceof LamExpression);
  }

  @Test
  public void inApp() {
    checkAsLam("\\Set0 -> \\Set1", "Path (\\lam _ => \\Set0) __ Nat");
  }

  @Test
  public void inProj() {
    typeCheckDef("\\func test : (\\Sigma Nat Nat) -> Nat => __.1");
    typeCheckDef("\\func test : (\\Sigma Nat Nat) -> Nat => __.2");
    typeCheckDef("\\func test : (\\Sigma Nat) -> Nat => __.1");
  }

  @Test
  public void notAllowedHole() {
    typeCheckDef("\\func test => __", 1);
  }

  @Test
  public void tupleTest() {
    typeCheckDef("\\func test : Nat -> \\Sigma Nat Nat => (0, __)");
  }

  @Test
  public void combinedTest() {
    checkAsLam("\\Sigma Nat Nat -> Nat", "1 Nat.+ __.1");
  }

  @Test
  public void combinedTest2() {
    typeCheckDef("\\func test (t1 t2 : \\Sigma (Nat -> Nat) Nat) (p : t1 = t2) => path ((p @ __).1 0)");
  }

  @Test
  public void appProjTest() {
    typeCheckDef("\\func test (f : (\\Sigma Nat Nat -> Nat) -> Nat) => f __.1");
  }

  @Test
  public void tupleAppProjTest() {
    typeCheckDef("\\func test (f : (\\Sigma Nat Nat -> Nat) -> Nat -> Nat) => (f __.1 0, 1)");
  }

  @Test
  public void appAppProjTest() {
    typeCheckDef("\\func test (f : (\\Sigma Nat Nat -> Nat) -> Nat) => suc (f __.1)");
  }

  @Test
  public void classExtTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func test : Nat -> Nat -> R => \\new R __ { | y => __ }");
  }

  @Test
  public void classExtTest2() {
    typeCheckModule(
      "\\record R (x : \\Sigma Nat Nat -> Nat)\n" +
      "\\func test => \\new R { | x => __.1 }");
  }

  @Test
  public void projAppTest() {
    typeCheckDef("\\func test (f : Nat -> \\Sigma Nat Nat) : Nat -> Nat => (f __).1");
  }

  @Test
  public void appPiTest1() {
    typeCheckDef("\\func test (f : (\\Set -> \\Set) -> \\Set) => f (__ -> Nat) -> Nat");
  }

  @Test
  public void appPiTest2() {
    typeCheckDef("\\func test (f : (\\Set -> \\Set) -> Nat) => f (__ -> Nat) = 0");
  }

  @Test
  public void appPiTest3() {
    typeCheckDef("\\func test (f : (Nat -> \\Set) -> Nat) => f (__ = 0 -> Nat)");
  }

  @Test
  public void appPiTest4() {
    typeCheckDef("\\func test (f : (Nat -> \\Set) -> Nat) (g : Nat -> \\Set) => f (g __ -> Nat)");
  }

  @Test
  public void appPiTest5() {
    typeCheckDef("\\func test (f : (Nat -> Nat -> \\Set -> \\Set) -> Nat) (g : Nat -> Nat) => f (g __ = g __ -> __)");
  }

  @Test
  public void appCaseTest() {
    typeCheckDef("\\func test (g : Nat -> Nat) (f : (Nat -> Nat) -> Nat) => g (f (\\case __ \\with { | 0 => 0 | suc n => n }))");
  }

  @Test
  public void appCaseTest2() {
    typeCheckDef("\\func test (g : Nat -> Nat -> Nat) (f : (Nat -> Nat) -> Nat) => g (g (f (\\case __ \\with { | 0 => 0 | suc n => n })) 0)");
  }

  @Test
  public void appCaseTest3() {
    typeCheckDef("\\func test (f : (Nat -> Nat) -> Nat) => f (\\case __ \\with { | 0 => 0 | suc n => n }) Nat.+ 0");
  }

  @Test
  public void appCaseTest4() {
    typeCheckDef("\\func test (g : Nat -> Nat) (f : (Nat -> Nat) -> Nat) => g (f (\\case __ \\with { | 0 => 0 | suc n => n }) Nat.+ 0)");
  }
}
