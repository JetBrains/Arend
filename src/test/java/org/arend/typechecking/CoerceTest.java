package org.arend.typechecking;

import org.junit.Test;

import static org.arend.Matchers.typeMismatchError;

public class CoerceTest extends TypeCheckingTestCase {
  @Test
  public void coerceTop() {
    parseModule("\\use \\coerce f (n : Nat) : Nat => n", 1);
  }

  @Test
  public void coerceDynamic() {
    typeCheckModule(
      "\\record C (n : Nat) (m : Nat -> Nat) {\n" +
      "  \\use \\coerce f => n\n" +
      "  \\use \\coerce g => m\n" +
      "}\n" +
      "\\func f' (c : C) : Nat => c\n" +
      "\\func g' (c : C) : Nat -> Nat => c");
  }

  @Test
  public void coerceFunction() {
    resolveNamesDef(
      "\\func g => 0\n" +
      "  \\where \\use \\coerce f (n : Nat) : Nat => n", 1);
  }

  @Test
  public void coerceFromDef() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce fromNat (n : Nat) => con n\n" +
      "\\func f (n : Nat) : D => n");
  }

  @Test
  public void coerceFromDefError() {
    typeCheckModule(
      "\\data D Nat | con Nat\n" +
      "  \\where \\use \\coerce fromNat (n : Nat) : D 1 => con n\n" +
      "\\func f (n : Nat) : D 0 => n", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void coerceFromExpr() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce fromPi (p : Nat -> D) => p 0\n" +
      "\\func f : D => con");
  }

  @Test
  public void coerceFromExprError() {
    typeCheckModule(
      "\\data D | con Int\n" +
      "  \\where \\use \\coerce fromPi (p : Nat -> D) => p 0\n" +
      "\\func f : D => con", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void coerceToDef() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce toNat (d : D) : Nat | con n => n\n" +
      "\\func f (d : D) : Nat => d");
  }

  @Test
  public void coerceToDefError() {
    typeCheckModule(
      "\\data D Nat | con Nat\n" +
      "  \\where \\use \\coerce toNat (d : D 0) : Nat | con n => n\n" +
      "\\func f (d : D 1) : Nat => d", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void coerceToExpr() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce toSigma (d : D) : \\Sigma D D => (d,d)\n" +
      "\\func f (d : D) : \\Sigma D D => d");
  }

  @Test
  public void coerceToExprError() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce toSigma (d : D) : \\Sigma D D => (d,d)\n" +
      "\\func f (d : D) : \\Sigma D Nat => d", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void incorrectCoerceFrom() {
    resolveNamesDef(
      "\\data D | con\n" +
      "  \\where \\use \\coerce f : D => con", 1);
  }

  @Test
  public void incorrectCoerce() {
    typeCheckModule(
      "\\data D | con\n" +
      "  \\where \\use \\coerce f (n : Nat) : Nat => n", 1);
  }

  @Test
  public void bothCoerce() {
    typeCheckModule(
      "\\data D | con\n" +
      "  \\where \\use \\coerce f (d : D) : D => d", 1);
  }

  @Test
  public void recursiveCoerceFromDef() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce fromNat (n : Nat) => con n\n" +
      "\\data E | con' D\n" +
      "  \\where \\use \\coerce fromD (d : D) => con' d\n" +
      "\\func f (n : Nat) : E => n");
  }

  @Test
  public void recursiveCoerceToDef() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce toNat (d : D) : Nat | con n => n\n" +
      "\\data E | con' D\n" +
      "  \\where \\use \\coerce toD (e : E) : D | con' d => d\n" +
      "\\func f (e : E) : Nat => e");
  }

  @Test
  public void coerceSelfCall() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce f (n : Nat) : D => n", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void coerceFromDefWithParameters() {
    typeCheckModule(
      "\\data D Nat | con Nat\n" +
      "  \\where \\use \\coerce fromNat {p : Nat} (n : Nat) : D p => con n\n" +
      "\\func f : D 3 => 1");
  }

  @Test
  public void coerceFromDefWithParametersError() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\coerce fromNat {p : Nat} (n : Nat) : D => con n\n" +
      "\\func f : D => 1", 1);
  }

  @Test
  public void coerceToDefWithParameters() {
    typeCheckModule(
      "\\data D Nat | con Nat\n" +
      "  \\where \\use \\coerce toNat {p : Nat} (d : D p) : Nat | con n => n\n" +
      "\\func f (d : D 3) : Nat => d");
  }

  @Test
  public void coerceToDefWithExplicitParameters() {
    typeCheckModule(
      "\\data D Nat | con Nat\n" +
      "  \\where \\use \\coerce toNat (p : Nat) (d : D p) : Nat \\elim d | con n => n\n" +
      "\\func f (d : D 3) : Nat => d");
  }

  @Test
  public void coerceToDefWithParametersError() {
    typeCheckModule(
      "\\data D Nat | con Nat\n" +
      "  \\where \\use \\coerce toNat {p : Nat} (d : D p) : Nat | con n => n\n" +
      "\\func f : Nat => con 2", 1);
  }

  @Test
  public void coerceTypeSigma() {
    typeCheckModule(
      "\\class Class (X : \\Type) (x : X)\n" +
      "\\func f (C : Class) => (\\Sigma (c : C) (c = c)) = (\\Sigma)");
  }
}
