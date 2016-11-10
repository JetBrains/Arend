package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TypeCheckingTest extends TypeCheckingTestCase {
  @Test
  public void typeCheckDefinition() {
    typeCheckClass(
        "\\function x : Nat => zero\n" +
        "\\function y : Nat => x");
  }

  @Test
  public void typeCheckDefType() {
    typeCheckClass(
        "\\function x : \\Type0 => Nat\n" +
        "\\function y : x => zero");
  }

  @Test
  public void typeCheckInfixDef() {
    typeCheckClass(
        "\\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
        "\\function (*) : Nat -> Nat => \\lam x => x + zero");
  }

  @Test
  public void typeCheckConstructor1() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {1} {2} {3} = (D 1 {2} 3).con => idp");
  }

  @Test
  public void typeCheckConstructor1d() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {1} {2} {3} = (D 1 {2} 3).con => idp");
  }

  @Test
  public void typeCheckConstructor2() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {0} (path (\\lam _ => 1)) = (D 0).con idp => idp");
  }

  @Test
  public void typeCheckConstructor2d() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {0} (path (\\lam _ => 1)) = (D 0).con idp => idp");
  }

  @Test
  public void testEither() {
    typeCheckClass(
        "\\data Either (A B : \\Type0) | inl A | inr B\n" +
        "\\function fun {A B : \\Type0} (e : Either A B) : \\Set0 <= \\elim e\n" +
        "  | inl _ => Nat\n" +
        "  | inr _ => Nat\n" +
        "\\function test : fun (inl {Nat} {Nat} 0) => 0");
  }

  @Test
  public void testPMap1() {
    typeCheckDef("\\function pmap {A B : \\Type1} {a a' : A} (f : A -> B) (p : a = a') : (f a = f a') => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testPMap1Mix() {
    typeCheckDef("\\function pmap {A : \\Type1} {B : \\Type0} {a a' : A} (f : A -> B) (p : a = a') : (f a = f a') => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testPMap1Error() {
    typeCheckDef("\\function pmap {A B : \\Type0} {a a' : A} (f : A -> B) (p : a = a') : ((=) {B} (f a) (f a'))" +
            " => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testTransport1() {
    typeCheckDef("\\function transport {A : \\Type1} (B : A -> \\Type1) {a a' : A} (p : a = a') (b : B a) : B a' =>\n" +
        "coe (\\lam i => B ((@) {\\lam _ => A} {a} {a'} p i)) b right");
  }

  @Test
  public void testTransport1Error() {
    typeCheckDef("\\function transport {A : \\Type1} (B : A -> \\Type1) {a a' : A} (p : a = a') (b : B a) : B a' =>\n" +
        "coe (\\lam i => B ((@) [zero] [inf] {\\lam _ => A} {a} {a'} p i)) b right", 1);
  }

  @Test
  public void testAt() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (p : suc = suc) => (p @ left) 0", null);
    assertNotNull(result.getExpression().getType());
  }

  @Test
  public void compareData() {
    typeCheckClass(
        "\\data D | con\n" +
        "\\function f {l : Lvl} (d : D [l] [0]) => d\n" +
        "\\function g {l : Lvl} (d : D [l] [inf]) => f d");
  }

  @Test
  public void universeInference() {
    typeCheckClass(
        "\\function\n" +
        "transport {lp : Lvl} {lh : CNat} {A : \\Type (lp,lh)} (B : A -> \\Type (lp,lh)) {a a' : A} (p : a = a') (b : B a)\n" +
        "  <= coe (\\lam i => B (p @ i)) b right\n" +
        "\n" +
        "\\function\n" +
        "foo (A : \\1-Type0) (B : A -> \\Type0) (a a' : A) (p : a = a') => transport B p");
  }

  @Test
  public void definitionsWithErrors() {
    typeCheckClass(
        "\\class C {\n" +
        "  \\field A : X\n" +
        "  \\field a : (\\lam (x : Nat) => Nat) A\n" +
        "}", 1, 2);
  }
}
