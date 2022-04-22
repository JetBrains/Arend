package org.arend.typechecking.definition;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeTest extends TypeCheckingTestCase {
  @Test
  public void coerceToTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => \\Sigma A A\n" +
      "\\func test : E Nat => (0,1)");
  }

  @Test
  public void coerceToTest2() {
    typeCheckModule(
      "\\type E (A : \\Type) => A -> A\n" +
      "\\func test : E Nat => \\lam x => x");
  }

  @Test
  public void coerceFromTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => A\n" +
      "\\func test (x : E Nat) : Nat => x");
  }

  @Test
  public void projTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => \\Sigma A A\n" +
      "\\func test (x : E Nat) => x.1");
  }

  @Test
  public void appTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => A -> A\n" +
      "\\func test (x : E Nat) => x 0");
  }

  @Test
  public void appImplicitTest() {
    typeCheckModule(
      "\\type E (A : \\Type) (a : A) => \\Pi {a' : A} -> a = a' -> Nat\n" +
      "\\func test (x : E Nat 0) => x idp");
  }

  @Test
  public void appImplicitTest2() {
    typeCheckModule(
      "\\type E (A : \\Type) (a : A) => \\Pi {a' : A} -> a = a' -> Nat\n" +
      "\\func test (x : E Nat 0) => x {_} idp");
  }

  @Test
  public void implicitTest() {
    typeCheckModule(
      "\\type E (A : \\Type) (a : A) => \\Pi {a' : A} -> a = a' -> Nat\n" +
      "\\func test (x : E Nat 0) : 0 = 0 -> Nat => x");
  }

  @Test
  public void tupleTest() {
    typeCheckModule(
      "\\type E (A : \\Type) (y : A) => \\Sigma (x : A) (x = y)\n" +
      "\\func test : E Nat 0 => (0,idp)");
  }

  @Test
  public void transitiveToTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => A -> A\n" +
      "\\type F (A : \\Type) => E (\\Sigma A A)\n" +
      "\\func test : F Nat => \\lam (p : \\Sigma Nat Nat) => (p.2,p.1)");
  }

  @Test
  public void transitiveFromTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => A\n" +
      "\\type F (A : \\Type) => E A\n" +
      "\\func test (x : F Nat) : Nat => x");
  }

  @Test
  public void embeddedToTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => A -> A\n" +
      "\\type F (A : \\Type) => \\Sigma A (E (\\Sigma A A))\n" +
      "\\func test : F Nat => (0, \\lam (p : \\Sigma Nat Nat) => (p.2,p.1))");
  }

  @Test
  public void embeddedFromTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => A -> A\n" +
      "\\type F (A : \\Type) => \\Sigma A (E (\\Sigma A A))\n" +
      "\\func test (x : F Nat) => (x.2 (0,1)).1");
  }

  @Test
  public void bidirectionalTest() {
    typeCheckModule(
      "\\type D (A : \\Type) => A\n" +
      "\\type E1 (A : \\Type) => D A\n" +
      "\\type E2 (A : \\Type) => E1 A\n" +
      "\\type E3 (A : \\Type) => E2 A\n" +
      "\\type F1 (A : \\Type) => D A\n" +
      "\\type F2 (A : \\Type) => F1 A\n" +
      "\\func test1 (x : F2 Nat) : E3 Nat => x\n" +
      "\\func test2 (x : E3 Nat) : F2 Nat => x");
  }

  @Test
  public void instanceTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => A\n" +
      "\\class C (X : \\Type) | field : X\n" +
      "\\instance c : C (E Nat) 0\n" +
      "\\func test : E Nat => field");
  }

  @Test
  public void lamTest() {
    typeCheckModule(
      "\\type E (A : \\Type) => A -> A\n" +
      "\\func test : E Nat => \\lam n => \\case n \\with { | 0 => 0 | suc n => n }");
  }

  @Test
  public void lamTest2() {
    typeCheckModule(
      "\\type E (A : \\Type) => A -> A\n" +
      "\\func test : Nat -> E Nat => \\lam m n => \\case n \\with { | 0 => 0 | suc n => n }");
  }

  @Test
  public void constructorTest() {
    typeCheckModule(
      "\\data D (A : \\Type) | con (A -> A)\n" +
      "\\type E => D (\\Sigma Nat Nat)\n" +
      "\\func test : E => con (\\lam p => (p.2,p.1))");
  }

  @Test
  public void patternMatchingTest() {
    typeCheckModule(
      "\\type E => Nat\n" +
      "\\func f (x : E) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc _ => 7\n" +
      "\\func test : f 3 = 7 => idp");
  }

  @Test
  public void patternMatchingTest2() {
    typeCheckModule(
      "\\data D | con1 | con2\n" +
      "\\type E => D\n" +
      "\\func f (x : E) : Nat\n" +
      "  | con1 => 2\n" +
      "  | con2 => 5\n" +
      "\\func test : f con1 = 2 => idp");
  }

  @Test
  public void emptyPatternMatchingTest() {
    typeCheckModule(
      "\\data Empty\n" +
      "\\type E => Empty\n" +
      "\\func test (x : E) : Nat");
  }

  @Test
  public void etaTest() {
    typeCheckModule(
      "\\type E => \\Sigma Nat Nat\n" +
      "\\func test (x : E) : x = (x.1,x.2) => idp");
  }

  @Test
  public void etaTest2() {
    typeCheckModule(
      "\\type E => \\Sigma Nat Nat\n" +
      "\\func test (x : E) : (x.1,x.2) = x => idp");
  }

  @Test
  public void missingClausesTest() {
    typeCheckModule(
      "\\type MyNat => Nat\n" +
      "\\func test (x : MyNat) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(2));
  }

  @Test
  public void letPatternTest() {
    typeCheckModule(
      "\\type E => \\Sigma Nat Nat\n" +
      "\\func test (x : E) => \\let (a,_) => x \\in a");
  }

  @Test
  public void letPatternRecursiveTest() {
    typeCheckModule(
      "\\type E => \\Sigma Nat Nat\n" +
      "\\type F => \\Sigma E Nat\n" +
      "\\func test (x : F) => \\let ((_,a),_) => x \\in a");
  }

  @Test
  public void recordTest() {
    typeCheckModule(
      "\\record R (a x y : Nat)\n" +
      "\\type S => R 0\n" +
      "\\func test : S => \\new R { | x => 0 | y => 0 }");
  }

  @Test
  public void recordEtaTest() {
    typeCheckModule(
      "\\record R (a x y : Nat)\n" +
      "\\type S => R 0\n" +
      "\\func test1 (s : S) : s = \\new R 0 s.x s.y => idp\n" +
      "\\func test2 (s : S) : s = \\new R s.a s.x s.y => idp");
  }

  @Test
  public void newExtTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\record S (r : R) (a : Nat)\n" +
      "\\type R' => R\n" +
      "\\func wrap (r : R) : R' => r\n" +
      "\\func test (s : S) : R s.r.x => wrap s.r");
  }
}
