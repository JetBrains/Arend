package org.arend.typechecking.patternmatching;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class DisjointConstructorsTest extends TypeCheckingTestCase {
  @Test
  public void disjoint() {
    typeCheckModule(
      "\\data D | con1 | con2\n" +
      "\\func f (p : con1 = con2) : Nat");
  }

  @Test
  public void sameConstructors() {
    typeCheckModule(
      "\\data D | con1 | con2\n" +
      "\\func f (p : con1 = con1) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void disjointHeterogeneous() {
    typeCheckModule(
      "\\data D | con1 | con2\n" +
      "\\func f (q : D = D) (p : Path (q @) con1 con2) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void integers() {
    typeCheckDef("\\func f (p : 1 = 2) : Nat");
  }

  @Test
  public void sameIntegers() {
    typeCheckDef("\\func f (p : 1 = 1) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void integersCon() {
    typeCheckDef("\\func f (x : Nat) (p : suc (suc x) = 1) : Nat");
  }

  @Test
  public void integersSameCon() {
    typeCheckDef("\\func f (x : Nat) (p : suc (suc x) = 2) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void recursive() {
    typeCheckModule(
      "\\data D | con1 D | con2\n" +
      "\\func f (p : con1 (con1 con2) = con1 con2) : Nat");
  }

  @Test
  public void recursiveSame() {
    typeCheckModule(
      "\\data D | con1 D | con2\n" +
      "\\func f (p : con1 (con1 con2) = con1 (con1 con2)) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void notConstructor() {
    typeCheckModule(
      "\\data D | con1 | con2\n" +
      "\\func f (x : D) (p : con1 = x) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void notConstructorRecursive() {
    typeCheckModule(
      "\\data D | con1 D | con2\n" +
      "\\func f (x : D) (p : con1 x = con1 con2) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void absurdPattern() {
    typeCheckModule(
      "\\data D | con1 | con2\n" +
      "\\data E | con (con1 = con2)\n" +
      "\\func f (e : E) : Nat\n" +
      "  | con ()");
  }

  @Test
  public void hitConstructors() {
    typeCheckModule(
      "\\data D | con1 | con2 | seg I \\with { | left => con1 | right => con2 }\n" +
      "\\func f (p : con1 = con2) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void recursiveHitConstructors() {
    typeCheckModule(
      "\\data D | con1 | con2 | seg (d : D) (i : I) \\elim i { | left => d | right => con2 }\n" +
      "\\func f (p : con1 = con2) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void hitDisjointConstructors() {
    typeCheckModule(
      "\\data D | con1 | con2 | con3 | seg I \\with { | left => con1 | right => con2 }\n" +
      "\\func f (p : con1 = con3) : Nat");
  }

  @Test
  public void hitTransitivelyHomotopicConstructors() {
    typeCheckModule(
      "\\data D | con1 | con2 | con3 | seg1 I \\with { | left => con1 | right => con2 } | seg2 I \\with { | left => con1 | right => con3 }\n" +
      "\\func f (p : con2 = con3) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void intDisjointConstructors() {
    typeCheckModule("\\func f (n : Nat) (p : pos n = neg (suc n)) : Nat");
  }

  @Test
  public void intDisjointConstructors2() {
    typeCheckModule("\\func f (n : Nat) (p : pos (suc n) = neg n) : Nat");
  }

  @Test
  public void intDisjointConstructors3() {
    typeCheckModule("\\func f (n : Nat) (p : pos n = neg n) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void intDisjointConstructors4() {
    typeCheckModule("\\func f (n m : Nat) (p : neg n = pos m) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(2));
  }

  @Test
  public void conditionsTest() {
    typeCheckModule(
      "\\data D | con1 | con2 (n : Nat) \\with { | 0 => con1 } | con3 (n : Nat) \\with { | suc n => con1 }\n" +
      "\\func f (n : Nat) (p : con2 n = con3 n) : Nat \\elim p", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void conditionsTest2() {
    typeCheckModule(
      "\\data D | con1 | con2 (n : Nat) \\with { | 0 => con1 } | con3 (n : Nat) \\with { | suc n => con1 }\n" +
      "\\func f (n : Nat) (p : con2 n = con3 0) : Nat");
  }

  @Test
  public void conditionsTest3() {
    typeCheckModule(
      "\\data D | con1 | con2 (n : Nat) \\with { | 0 => con1 } | con3 (n : Nat) \\with { | suc n => con1 }\n" +
      "\\func f (p : con2 0 = con3 0) : Nat");
  }

  @Test
  public void conditionsTest4() {
    typeCheckModule(
      "\\data D | con1 Nat | con2 (n : Nat) \\with { | suc n => con1 0 } | con3 (n : Nat) \\with { | suc n => con1 (suc n) }\n" +
      "\\func f (n m : Nat) (p : con2 n = con3 m) : Nat");
  }

  @Test
  public void conditionsTest5() {
    typeCheckModule(
      "\\data D | con1 Nat | con2 (n : Nat) \\with { | suc n => con1 0 } | con3 (n : Nat) \\with { | suc n => con1 n }\n" +
      "\\func f (n m : Nat) (p : con2 n = con3 m) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(2));
  }
}
