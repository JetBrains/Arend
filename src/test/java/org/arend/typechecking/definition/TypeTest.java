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
      """
        \\type E (A : \\Type) => A -> A
        \\type F (A : \\Type) => E (\\Sigma A A)
        \\func test : F Nat => \\lam (p : \\Sigma Nat Nat) => (p.2,p.1)
        """);
  }

  @Test
  public void transitiveFromTest() {
    typeCheckModule(
      """
        \\type E (A : \\Type) => A
        \\type F (A : \\Type) => E A
        \\func test (x : F Nat) : Nat => x
        """);
  }

  @Test
  public void embeddedToTest() {
    typeCheckModule(
      """
        \\type E (A : \\Type) => A -> A
        \\type F (A : \\Type) => \\Sigma A (E (\\Sigma A A))
        \\func test : F Nat => (0, \\lam (p : \\Sigma Nat Nat) => (p.2,p.1))
        """);
  }

  @Test
  public void embeddedFromTest() {
    typeCheckModule(
      """
        \\type E (A : \\Type) => A -> A
        \\type F (A : \\Type) => \\Sigma A (E (\\Sigma A A))
        \\func test (x : F Nat) => (x.2 (0,1)).1
        """);
  }

  @Test
  public void bidirectionalTest() {
    typeCheckModule(
      """
        \\type D (A : \\Type) => A
        \\type E1 (A : \\Type) => D A
        \\type E2 (A : \\Type) => E1 A
        \\type E3 (A : \\Type) => E2 A
        \\type F1 (A : \\Type) => D A
        \\type F2 (A : \\Type) => F1 A
        \\func test1 (x : F2 Nat) : E3 Nat => x
        \\func test2 (x : E3 Nat) : F2 Nat => x
        """);
  }

  @Test
  public void instanceTest() {
    typeCheckModule(
      """
        \\type E (A : \\Type) => A
        \\class C (X : \\Type) | field : X
        \\instance c : C (E Nat) 0
        \\func test : E Nat => field
        """);
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
      """
        \\data D (A : \\Type) | con (A -> A)
        \\type E => D (\\Sigma Nat Nat)
        \\func test : E => con (\\lam p => (p.2,p.1))
        """);
  }

  @Test
  public void patternMatchingTest() {
    typeCheckModule(
      """
        \\type E => Nat
        \\func f (x : E) : Nat
          | 0 => 0
          | suc _ => 7
        \\func test : f 3 = 7 => idp
        """);
  }

  @Test
  public void patternMatchingTest2() {
    typeCheckModule(
      """
        \\data D | con1 | con2
        \\type E => D
        \\func f (x : E) : Nat
          | con1 => 2
          | con2 => 5
        \\func test : f con1 = 2 => idp
        """);
  }

  @Test
  public void patternMatchingTest3() {
    typeCheckModule(
      """
        \\type Nat' => Nat
        \\lemma test (n m : Nat') : n Nat.+ m = m Nat.+ n \\elim n
          | 0 => idp
          | suc n => path (\\lam i => suc (test n m @ i))
        """);
  }

  @Test
  public void emptyPatternMatchingTest() {
    typeCheckModule(
      """
        \\data Empty
        \\type E => Empty
        \\func test (x : E) : Nat
        """);
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
      """
        \\type E => \\Sigma Nat Nat
        \\type F => \\Sigma E Nat
        \\func test (x : F) => \\let ((_,a),_) => x \\in a
        """);
  }

  @Test
  public void recordTest() {
    typeCheckModule(
      """
        \\record R (a x y : Nat)
        \\type S => R 0
        \\func test : S => \\new R { | x => 0 | y => 0 }
        """);
  }

  @Test
  public void recordEtaTest() {
    typeCheckModule(
      """
        \\record R (a x y : Nat)
        \\type S => R 0
        \\func test1 (s : S) : s = \\new R 0 s.x s.y => idp
        \\func test2 (s : S) : s = \\new R s.a s.x s.y => idp
        """);
  }

  @Test
  public void newExtTest() {
    typeCheckModule(
      """
        \\record R (x y : Nat)
        \\record S (r : R) (a : Nat)
        \\type R' => R
        \\func wrap (r : R) : R' => r
        \\func test (s : S) : R s.r.x => wrap s.r
        """);
  }

  @Test
  public void levelsTest() {
    typeCheckModule(
      "\\type Type => \\Set\n" +
      "\\func test (A : Type \\levels 2 _) : Type \\levels 1 _ => A", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void mutuallyRecursive() {
    typeCheckModule(
      "\\data D : \\Set | con f\n" +
      "\\type f : \\Set => D -> Nat", 1);
  }
}
