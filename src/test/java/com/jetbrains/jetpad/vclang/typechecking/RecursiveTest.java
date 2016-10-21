package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecursiveTest extends TypeCheckingTestCase {
  @Test
  public void list() {
    assertTrue(typeCheckDef("\\data List (A : \\Type0) | nil | cons A (List A)").hasErrors() == Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void dataLeftError() {
    Definition def = typeCheckDef("\\data List (A : \\Type0) | nil | cons (List A -> A)", 1);
    assertTrue(def.hasErrors() == Definition.TypeCheckingStatus.HAS_ERRORS);
    assertFalse(def.typeHasErrors());
  }

  @Test
  public void dataRightError() {
    Definition def = typeCheckDef("\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))", 1);
    assertTrue(def.hasErrors() == Definition.TypeCheckingStatus.HAS_ERRORS);
    assertFalse(def.typeHasErrors());
  }

  @Test
  public void plus() {
    assertTrue(typeCheckDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => suc (x' + y)").hasErrors() == Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void doubleRec() {
    assertTrue(typeCheckDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc zero => y | suc (suc x'') => x'' + (x'' + y)").hasErrors() == Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void functionError() {
    assertTrue(typeCheckDef("\\function (+) (x y : Nat) : Nat <= x + y", 1).hasErrors() == Definition.TypeCheckingStatus.HAS_ERRORS);
  }

  @Test
  public void functionError2() {
    assertTrue(typeCheckDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc zero => y | suc (suc x'') => y + y", 1).hasErrors() == Definition.TypeCheckingStatus.HAS_ERRORS);
  }

  @Test
  public void functionPartiallyApplied() {
    assertTrue(typeCheckDef("\\function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => z (foo z x')").hasErrors() == Definition.TypeCheckingStatus.NO_ERRORS);
  }
}
