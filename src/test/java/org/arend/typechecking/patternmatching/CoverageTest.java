package org.arend.typechecking.patternmatching;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.Matchers.missingClauses;

public class CoverageTest extends TypeCheckingTestCase {
  @Test
  public void coverageInCase() {
    typeCheckDef("\\func test : Nat => \\case 1 \\with { zero => 0 }", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void coverageTest() {
    typeCheckModule(
        "\\data Fin Nat \\with | _ => fzero | suc n => fsuc (Fin n)\n" +
        "\\func unsuc {n : Nat} (x : Fin n) : Fin n \\elim n, x\n" +
        "  | zero, fzero => fzero\n" +
        "  | suc n, fzero => fzero\n" +
        "  | suc n, fsuc x => fsuc (unsuc x)");
  }

  @Test
  public void coverageTest2() {
    typeCheckModule(
        "\\data Fin Nat \\with | _ => fzero | suc n => fsuc (Fin n)\n" +
        "\\func unsuc {n : Nat} (x : Fin n) : Fin n \\elim n, x\n" +
        "  | _, fzero => fzero\n" +
        "  | suc n, fsuc x => fsuc (unsuc x)");
  }

  @Test
  public void conditionsCoverage() {
    typeCheckModule(
      "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
      "\\func f (z : Z) (n : Nat) : Nat\n" +
      "  | pos zero, zero => zero\n" +
      "  | pos zero, suc n => n\n" +
      "  | pos (suc k), _ => k\n" +
      "  | neg (suc k), _ => k");
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
    assertThatErrorsAre(missingClauses(3));
  }

  @Test
  public void missingAbsurdPattern() {
    typeCheckModule(
      "\\data Empty\n" +
      "\\func \\infix 4 isNeg (x : Nat) : \\Type\n" +
      "  | 0 => Empty\n" +
      "  | suc x => Empty\n" +
      "\\func test {n : Nat} (p : isNeg n) : Empty", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void missingClausesTest() {
    typeCheckModule(
      "\\data Fin (n : Nat) \\with\n" +
      "  | suc n => { | fzero | fsuc (Fin n) }\n" +
      "\\func test (n : Nat) (x : Fin n) : Nat \\elim n, x", 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void missingHigherConstructorsTest() {
    typeCheckModule(
      "\\data S1 | base | loop I \\with { | left => base | right => base }\n" +
      "\\func test (x : S1) : \\Sigma", 1);
    assertThatErrorsAre(missingClauses(1));
  }
}
