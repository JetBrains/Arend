package org.arend.typechecking.patternmatching;

import org.arend.Matchers;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.error.MissingClausesError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.List;

import static org.arend.Matchers.missingClauses;
import static org.junit.Assert.assertEquals;

public class CoverageTest extends TypeCheckingTestCase {
  @Test
  public void coverageInCase() {
    typeCheckDef("\\func test : Nat => \\case 1 \\with { zero => 0 }", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void coverageTest() {
    typeCheckModule(
      """
        \\data Fin Nat \\with | _ => fzero | suc n => fsuc (Fin n)
        \\func unsuc {n : Nat} (x : Fin n) : Fin n \\elim n, x
          | zero, fzero => fzero
          | suc n, fzero => fzero
          | suc n, fsuc x => fsuc (unsuc x)
        """);
  }

  @Test
  public void coverageTest2() {
    typeCheckModule(
      """
        \\data Fin Nat \\with | _ => fzero | suc n => fsuc (Fin n)
        \\func unsuc {n : Nat} (x : Fin n) : Fin n \\elim n, x
          | _, fzero => fzero
          | suc n, fsuc x => fsuc (unsuc x)
        """);
  }

  @Test
  public void conditionsCoverage() {
    typeCheckModule(
      """
        \\data Z | pos Nat | neg Nat { zero => pos zero }
        \\func f (z : Z) (n : Nat) : Nat
          | pos zero, zero => zero
          | pos zero, suc n => n
          | pos (suc k), _ => k
          | neg (suc k), _ => k
        """);
  }

  @Test
  public void emptyCoverage() {
    typeCheckDef("\\func foo (x : Nat) : Nat", 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void twoVariablesEmptyCoverage() {
    typeCheckDef("\\func foo (x y : Nat) : Nat", 1);
    assertThatErrorsAre(missingClauses(4));
  }

  @Test
  public void elimEmptyCoverage() {
    typeCheckDef("\\func foo (x y : Nat) : Nat \\elim x", 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void emptyCoverageWithIndices() {
    typeCheckModule(
      "\\data Fin Nat \\with | _ => fzero | suc n => fsuc (Fin n)\n" +
      "\\func unsuc {n : Nat} (x : Fin n) : Fin n", 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void missingAbsurdPattern() {
    typeCheckModule(
      """
        \\data Empty
        \\func \\infix 4 isNeg (x : Nat) : \\Type
          | 0 => Empty
          | suc x => Empty
        \\func test {n : Nat} (p : isNeg n) : Empty
        """, 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void missingClausesTest() {
    typeCheckModule(
      """
        \\data Fin (n : Nat) \\with
          | suc n => { | fzero | fsuc (Fin n) }
        \\func test (n : Nat) (x : Fin n) : Nat \\elim n, x
        """, 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void missingHigherConstructorsTest() {
    typeCheckModule(
      "\\data S1 | base | loop I \\with { | left => base | right => base }\n" +
      "\\func test (x : S1) : \\Sigma", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void matchingDataTest() {
    typeCheckModule(
      """
        \\data D (n : Nat) \\with
          | 0 => con1
          | suc n => con2
        \\func test (n : Nat) (x : D n) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void matchingDataTest1() {
    typeCheckModule(
      """
        \\data D (n : Nat) \\with
          | 0 => { con1 | con1' }
          | suc n => con2
        \\func test (n : Nat) (x : D n) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(3));
  }

  @Test
  public void matchingDataTest2() {
    typeCheckModule(
      """
        \\data D (n : Nat) \\with
          | 0 => con1
          | suc n => con2
        \\func test (n : Nat) (x : D (suc n)) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void matchingDataTest3() {
    typeCheckModule(
      """
        \\data D (n : Nat) \\with
          | 0 => con1
          | suc n => con2
        \\func test (n : Nat) (x : D (suc (suc n))) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void matchingDataTest4() {
    typeCheckModule(
      """
        \\data D (n : Nat) \\with
          | 0 => con1
          | suc n => con2
        \\func test (n : Nat) (x y : D n) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void matchingDataTest5() {
    typeCheckModule(
      """
        \\data D (n : Nat) \\with
          | 0 => con1
          | suc n => con2
        \\func test (n : Nat) (x : D (n Nat.+ n)) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void matchingDataTest6() {
    typeCheckModule(
      """
        \\data D (n : Nat) \\with
          | 0 => con1
          | suc n => con2
        \\func test (n : Nat) (x : D (suc (suc n))) : Nat \\elim x
        """, 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void matchingDataTest7() {
    typeCheckModule(
      """
        \\data D (n m : Nat) \\with
          | 0, 0 => con1
          | suc _, suc _ => con2
          | suc (suc _), suc (suc _) => con3
          | _, _ => con4
        \\func foo (n m : Nat) (d : D n m) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(4));
  }

  @Test
  public void matchingDataTest8() {
    typeCheckModule(
      """
        \\data D {A : \\Type} (n m : Nat) \\with
          | 0, 0 => con1
          | suc _, suc _ => con2
          | suc (suc _), suc (suc _) => con3
          | _, _ => con4
        \\func foo {A : \\Type} {n m : Nat} (d : D {A} n m) : Nat \\elim n, m, d
        """, 1);
    assertThatErrorsAre(missingClauses(4));
  }

  @Test
  public void repeatedVarTest() {
    typeCheckModule(
      """
        \\data Bool | true | false
        \\data \\infix 4 < (x y : Bool) \\with
          | false, true => false<true
        \\func test {A : \\Type} (b : Bool) (p : b < b) : A \\elim b, p
          | true, ()
        """);
  }

  @Test
  public void repeatedVarTest2() {
    typeCheckModule(
      """
        \\data Bool | true | false
        \\data \\infix 4 < (x y : Bool) \\with
          | false, true => false<true
        \\func test {A : \\Type} (b : Bool) (p : b < b) : A
          | true, ()
        """);
  }

  @Test
  public void repeatedVarError() {
    typeCheckModule(
      """
        \\data Bool | true | false
        \\data \\infix 4 < (x y : Bool) \\with
          | true, true => true<true
        \\func test {A : \\Type} (b : Bool) (p : b < b) : A \\elim b, p
        """, 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void someTest() {
    typeCheckModule(
      """
        \\data Bool | true | false
        \\data \\infix 4 <= (x y : Bool) \\with
          | false, _ => false<=_
          | true, true => true<=true
        \\func test (x y : Bool) (p : x <= y) (q : y <= x) : x = y
        """, 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void anotherTest() {
    typeCheckModule(
      """
        \\data Bool | true | false
        \\data \\infix 4 < (x y : Bool) \\with
          | false, true => false<true
        \\func test (x y z : Bool) (p : x < y) (q : y < z) : x < z
        """);
  }

  @Test
  public void yetAnotherTest() {
    typeCheckModule(
      """
        \\data \\infix 4 <= (n m : Nat) \\with
          | 0, _ => zero<=_
          | suc n, suc m => suc<=suc (n <= m)
        \\func test (x y z : Nat) (p : y <= z) : Nat \\elim x
        """, 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void tupleTest() {
    typeCheckDef("\\func test (p : \\Sigma Nat Nat) : Nat", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void varTest() {
    typeCheckModule(
      """
        \\data T (n : Nat) \\with
          | 0 => conT
        \\data W (n : Nat) (y : T n) \\with
          | 0, y => conW
        \\func test (n : Nat) (y : T n) (w : W n y) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void varTest2() {
    typeCheckModule(
      """
        \\data T (n : Nat) \\with
          | 0 => con1
          | 0 => con2
        \\data W (n : Nat) (x y : T n) \\with
          | 0, con1, y => con
        \\func test (n : Nat) (x y : T n) (w : W n x y) : Nat
        """, 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void implicitPatternsInMissingClauses() {
    typeCheckDef(
      "\\func test (l : Array Nat) : Nat\n" +
      "  | nil => 0", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
    List<? extends CorePattern> patterns = ((MissingClausesError) errorList.get(0)).missingClauses.get(0);
    assertEquals(1, patterns.size());
    assertEquals(":: a l", patterns.get(0).prettyPrint(PrettyPrinterConfig.DEFAULT).toString());
  }

  @Test
  public void implicitPatternsInMissingClauses2() {
    typeCheckDef(
      """
        \\func test (l : Array Nat) : Nat
          | nil => 0
          | :: {0} a l => a
        """, 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
    List<? extends CorePattern> patterns = ((MissingClausesError) errorList.get(0)).missingClauses.get(0);
    assertEquals(1, patterns.size());
    assertEquals(":: {suc n} a l", patterns.get(0).prettyPrint(PrettyPrinterConfig.DEFAULT).toString());
  }
}
