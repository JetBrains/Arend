package org.arend.typechecking;

import org.arend.core.definition.Definition;
import org.arend.typechecking.error.TerminationCheckError;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class RecursiveTest extends TypeCheckingTestCase {
  @Test
  public void list() {
    assertSame(typeCheckDef("\\data List (A : \\Type0) | nil | cons A (List A)").status(), Definition.TypeCheckingStatus.NO_ERRORS);
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
    assertSame(typeCheckDef("\\func \\infixr 9 + (x y : Nat) : Nat \\elim x | zero => y | suc x' => suc (x' + y)").status(), Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void doubleRec() {
    assertSame(typeCheckDef("\\func \\infixr 9 + (x y : Nat) : Nat \\elim x | zero => y | suc zero => y | suc (suc x'') => x'' + (x'' + y)").status(), Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void functionError() {
    assertSame(typeCheckDef("\\func \\infixr 9 + (x y : Nat) : Nat => x + y", 1).status(), Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
  }

  @Test
  public void functionError2() {
    assertSame(typeCheckDef("\\func \\infixr 9 + (x y : Nat) : Nat \\elim x | zero => y | suc zero => y | suc (suc x'') => y + y", 1).status(), Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
  }

  @Test
  public void functionPartiallyApplied() {
    assertSame(typeCheckDef("\\func foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat \\elim x | zero => y | suc x' => z (foo z x')").status(), Definition.TypeCheckingStatus.NO_ERRORS);
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

  @Test
  public void mutualRecursionError() {
    typeCheckModule(
      "\\data D (n : Nat) : \\Type | con1 (f (\\lam (x : Nat) => x)) | con2\n" +
      "\\func f (n : Nat) : \\Type => D n\n" +
      "\\func g : f 0 => con2", 2);
  }

  @Test
  public void mutualRecursionOrder() {
    typeCheckModule(
      "\\func g => D'\n" +
      "\\data D : \\Type | con1 | con2 (d : D) (D' d)\n" +
      "\\data D' (d : D) : \\Type \\with\n" +
      "  | con1 => con1'\n" +
      "  | con2 _ _ => con2'");
  }
}
