package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class CoverageTest extends TypeCheckingTestCase {
  @Test
  public void coverageInCase() {
    typeCheckDef("\\function test : Nat => \\case 1 | zero => 0", 1);
  }

  @Test
  public void coverageTest() {
    typeCheckClass(
        "\\data Fin (n : Nat) | Fin n => fzero | Fin (suc n) => fsuc (Fin n)\n" +
        "\\function unsuc {n : Nat} (x : Fin n) : Fin n => \\elim n, x\n" +
        "  | zero, fzero => fzero\n" +
        "  | suc n, fzero => fzero\n" +
        "  | suc n, fsuc x => fsuc (unsuc x)");
  }

  @Test
  public void coverageTest2() {
    typeCheckClass(
        "\\data Fin (n : Nat) | Fin n => fzero | Fin (suc n) => fsuc (Fin n)\n" +
        "\\function unsuc {n : Nat} (x : Fin n) : Fin n => \\elim n, x\n" +
        "  | _, fzero => fzero\n" +
        "  | suc n, fsuc x => fsuc (unsuc x)");
  }
}
