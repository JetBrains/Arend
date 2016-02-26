package com.jetbrains.jetpad.vclang.typechecking;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;

public class DataIndicesTest {
  @Test
  public void vectorTest() {
    typeCheckClass(
      "\\static \\data Vector (n : Nat) (A : \\Type0)\n" +
      "  | Vector  zero   A => vnil\n" +
      "  | Vector (suc n) A => \\infixr 5 (:^) A (Vector n A)\n" +
      "\n" +
      "\\static \\function \\infixl 6\n" +
      "(+) (x y : Nat) : Nat <= \\elim x\n" +
      "  | zero => y\n" +
      "  | suc x' => suc (x' + y)\n" +
      "\n" +
      "\\static \\function\n" +
      "(+^) {n m : Nat} {A : \\Type0} (xs : Vector n A) (ys : Vector m A) : Vector (n + m) A <= \\elim n, xs\n" +
      "  | zero, vnil => ys\n" +
      "  | suc n', (:^) x xs' => x :^ xs' +^ ys\n" +
      "\n" +
      "\\static \\function\n" +
      "vnil-vconcat {n : Nat} {A : \\Type0} (xs : Vector n A) : vnil +^ xs = xs => path (\\lam _ => xs)");
  }

  @Test
  public void vectorTest2() {
    typeCheckClass(
      "\\static \\data Vector (n : Nat) (A : \\Type0)\n" +
      "  | Vector  zero   A => vnil\n" +
      "  | Vector (suc n) A => \\infixr 5 (:^) A (Vector n A)\n" +
      "\\static \\function id {n : Nat} (A : \\Type0) (v : Vector n A) => v\n" +
      "\\static \\function test => id Nat vnil");
  }
}
