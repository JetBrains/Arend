package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class CoverageTest extends TypeCheckingTestCase {
  @Test
  public void coverageInCase() {
    typeCheckDef("\\func test : Nat => \\case 1 \\with { zero => 0 }", 1);
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
}
