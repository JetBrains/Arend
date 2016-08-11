package com.jetbrains.jetpad.vclang.typechecking;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecursiveTest extends TypeCheckingTestCase {
  @Test
  public void list() {
    assertFalse(typeCheckDef("\\data List (A : \\Type0) | nil | cons A (List A)").hasErrors());
  }

  @Test
  public void dataLeftError() {
    assertFalse(typeCheckDef("\\data List (A : \\Type0) | nil | cons (List A -> A)", 1).hasErrors());
  }

  @Test
  public void dataRightError() {
    assertFalse(typeCheckDef("\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))", 1).hasErrors());
  }

  @Test
  public void plus() {
    assertFalse(typeCheckDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => suc (x' + y)").hasErrors());
  }

  @Test
  public void doubleRec() {
    assertFalse(typeCheckDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc zero => y | suc (suc x'') => x'' + (x'' + y)").hasErrors());
  }

  @Test
  public void functionError() {
    assertTrue(typeCheckDef("\\function (+) (x y : Nat) : Nat <= x + y", 1).hasErrors());
  }

  @Test
  public void functionError2() {
    assertTrue(typeCheckDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc zero => y | suc (suc x'') => y + y", 1).hasErrors());
  }

  @Test
  public void functionPartiallyApplied() {
    assertFalse(typeCheckDef("\\function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => z (foo z x')").hasErrors());
  }
}
