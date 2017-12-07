package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.error.TerminationCheckError;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecursiveTest extends TypeCheckingTestCase {
  @Test
  public void list() {
    assertTrue(typeCheckDef("\\data List (A : \\Type0) | nil | cons A (List A)").status() == Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void dataLeftError() {
    Definition def = typeCheckDef("\\data List (A : \\Type0) | nil | cons (List A -> A)", 1);
    assertEquals(Definition.TypeCheckingStatus.BODY_HAS_ERRORS, def.status());
  }

  @Test
  public void dataRightError() {
    Definition def = typeCheckDef("\\data List (B : \\oo-Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))", 1);
    assertEquals(Definition.TypeCheckingStatus.BODY_HAS_ERRORS, def.status());
  }

  @Test
  public void plus() {
    assertTrue(typeCheckDef("\\func \\infixr 9 + (x y : Nat) : Nat \\elim x | zero => y | suc x' => suc (x' + y)").status() == Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void doubleRec() {
    assertTrue(typeCheckDef("\\func \\infixr 9 + (x y : Nat) : Nat \\elim x | zero => y | suc zero => y | suc (suc x'') => x'' + (x'' + y)").status() == Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void functionError() {
    assertTrue(typeCheckDef("\\func \\infixr 9 + (x y : Nat) : Nat => x + y", 1).status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
  }

  @Test
  public void functionError2() {
    assertTrue(typeCheckDef("\\func \\infixr 9 + (x y : Nat) : Nat \\elim x | zero => y | suc zero => y | suc (suc x'') => y + y", 1).status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
  }

  @Test
  public void functionPartiallyApplied() {
    assertTrue(typeCheckDef("\\func foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat \\elim x | zero => y | suc x' => z (foo z x')").status() == Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void withType() {
    typeCheckDef("\\func f : Nat => f", 1);
    assertThatErrorsAre(instanceOf(TerminationCheckError.class));
  }

  @Test
  public void withoutType() {
    typeCheckDef("\\func f => f", 1);
  }
}
