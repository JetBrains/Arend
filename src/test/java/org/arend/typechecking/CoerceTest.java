package org.arend.typechecking;

import org.arend.typechecking.error.local.CoerceClashError;
import org.junit.Test;

import static org.arend.Matchers.typeMismatchError;
import static org.arend.Matchers.typecheckingError;

public class CoerceTest extends TypeCheckingTestCase {
  @Test
  public void coerceTop() {
    parseModule("\\use \\coerce f (n : Nat) : Nat => n", 1);
  }

  @Test
  public void coerceDynamic() {
    typeCheckModule(
      """
        \\record C (n : Nat) (m : Nat -> Nat) {
          \\use \\coerce f => n
          \\use \\coerce g => m
        }
        \\func f' (c : C) : Nat => c
        \\func g' (c : C) : Nat -> Nat => c
        """);
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
      """
        \\data D | con Nat
          \\where \\use \\coerce fromNat (n : Nat) => con n
        \\func f (n : Nat) : D => n
        """);
  }

  @Test
  public void coerceFromDefError() {
    typeCheckModule(
      """
        \\data D Nat | con Nat
          \\where \\use \\coerce fromNat (n : Nat) : D 1 => con n
        \\func f (n : Nat) : D 0 => n
        """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void coerceFromExpr() {
    typeCheckModule(
      """
        \\data D | con Nat
          \\where \\use \\coerce fromPi (p : Nat -> D) => p 0
        \\func f : D => con
        """);
  }

  @Test
  public void coerceFromExprError() {
    typeCheckModule(
      """
        \\data D | con Int
          \\where \\use \\coerce fromPi (p : Nat -> D) => p 0
        \\func f : D => con
        """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void coerceToDef() {
    typeCheckModule(
      """
        \\data D | con Nat
          \\where \\use \\coerce toNat (d : D) : Nat | con n => n
        \\func f (d : D) : Nat => d
        """);
  }

  @Test
  public void coerceToDefError() {
    typeCheckModule(
      """
        \\data D Nat | con Nat
          \\where \\use \\coerce toNat (d : D 0) : Nat | con n => n
        \\func f (d : D 1) : Nat => d
        """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void coerceToExpr() {
    typeCheckModule(
      """
        \\data D | con Nat
          \\where \\use \\coerce toSigma (d : D) : \\Sigma D D => (d,d)
        \\func f (d : D) : \\Sigma D D => d
        """);
  }

  @Test
  public void coerceToExprError() {
    typeCheckModule(
      """
        \\data D | con Nat
          \\where \\use \\coerce toSigma (d : D) : \\Sigma D D => (d,d)
        \\func f (d : D) : \\Sigma D Nat => d
        """, 1);
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
      """
        \\data D | con Nat
          \\where \\use \\coerce fromNat (n : Nat) => con n
        \\data E | con' D
          \\where \\use \\coerce fromD (d : D) => con' d
        \\func f (n : Nat) : E => n
        """);
  }

  @Test
  public void recursiveCoerceToDef() {
    typeCheckModule(
      """
        \\data D | con Nat
          \\where \\use \\coerce toNat (d : D) : Nat | con n => n
        \\data E | con' D
          \\where \\use \\coerce toD (e : E) : D | con' d => d
        \\func f (e : E) : Nat => e
        """);
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
      """
        \\data D Nat | con Nat
          \\where \\use \\coerce fromNat {p : Nat} (n : Nat) : D p => con n
        \\func f : D 3 => 1
        """);
  }

  @Test
  public void coerceFromDefWithParametersError() {
    typeCheckModule(
      """
        \\data D | con Nat
          \\where \\use \\coerce fromNat {p : Nat} (n : Nat) : D => con n
        \\func f : D => 1
        """, 1);
  }

  @Test
  public void coerceToDefWithParameters() {
    typeCheckModule(
      """
        \\data D Nat | con Nat
          \\where \\use \\coerce toNat {p : Nat} (d : D p) : Nat | con n => n
        \\func f (d : D 3) : Nat => d
        """);
  }

  @Test
  public void coerceToDefWithExplicitParameters() {
    typeCheckModule(
      """
        \\data D Nat | con Nat
          \\where \\use \\coerce toNat (p : Nat) (d : D p) : Nat \\elim d | con n => n
        \\func f (d : D 3) : Nat => d
        """);
  }

  @Test
  public void coerceToDefWithParametersError() {
    typeCheckModule(
      """
        \\data D Nat | con Nat
          \\where \\use \\coerce toNat {p : Nat} (d : D p) : Nat | con n => n
        \\func f : Nat => con 2
        """, 1);
  }

  @Test
  public void coerceTypeSigma() {
    typeCheckModule(
      "\\class Class (X : \\Type) (x : X)\n" +
      "\\func f (C : Class) => (\\Sigma (c : C) (c = c)) = (\\Sigma)");
  }

  @Test
  public void coerceField() {
    typeCheckModule(
      "\\record R (\\coerce f : Nat)\n" +
      "\\func foo (r : R) : Nat => r");
  }

  @Test
  public void coerceFieldClash() {
    typeCheckModule(
      "\\record R (\\coerce f : Nat)\n" +
      "  | \\coerce g : Nat", 1);
    assertThatErrorsAre(typecheckingError(CoerceClashError.class));
  }

  @Test
  public void twoCoerceFields() {
    typeCheckModule(
      """
        \\record R (\\coerce f : Nat)
          | \\coerce g : Int
        \\func foo (r : R) : Nat => r
        \\func bar (r : R) : Int => r
        """);
  }

  @Test
  public void coerceFieldSubclass() {
    typeCheckModule(
      "\\record R (\\coerce f : Nat)\n" +
      "\\record S \\extends R" +
      "\\func foo (s : S) : Nat => s");
  }

  @Test
  public void coerceConstructor() {
    typeCheckModule(
      """
        \\data D
          | \\coerce con Nat
        \\func foo (n : Nat) : D => n
        """);
  }

  @Test
  public void coerceConstructorClash() {
    typeCheckModule(
      """
        \\data D
          | \\coerce con1 Nat
          | \\coerce con2 Nat
        """, 1);
    assertThatErrorsAre(typecheckingError(CoerceClashError.class));
  }

  @Test
  public void twoCoerceConstructor() {
    typeCheckModule(
      """
        \\data D
          | \\coerce con1 Nat
          | \\coerce con2 Int
        \\func foo (n : Nat) : D => n
        \\func bar (x : Int) : D => x
        """);
  }

  @Test
  public void coerceFromPi() {
    typeCheckModule(
      """
        \\data D | con (Nat -> Nat)
          \\where \\use \\coerce fromPi (f : Nat -> Nat) => con f
        \\func f (f : Nat -> Nat) : D => f
        """);
  }

  @Test
  public void coerceToPi() {
    typeCheckModule(
      """
        \\data D | con (Nat -> Nat)
          \\where \\use \\coerce toPi (d : D) : Nat -> Nat
            | con f => f
        \\func f (d : D) : Nat -> Nat => d
        """);
  }

  @Test
  public void coerceToPiArg() {
    typeCheckModule(
      """
        \\data D | con (Nat -> Nat)
          \\where \\use \\coerce toPi (d : D) : Nat -> Nat
            | con f => f
        \\func f (d : D) => d 0
        """);
  }

  @Test
  public void coerceFromSigma() {
    typeCheckModule(
      """
        \\data D | con (\\Sigma Nat Nat)
          \\where \\use \\coerce fromSigma (p : \\Sigma Nat Nat) => con p
        \\func f (p : \\Sigma Nat Nat) : D => p
        """);
  }

  @Test
  public void coerceToSigma() {
    typeCheckModule(
      """
        \\data D | con (\\Sigma Nat Nat)
          \\where \\use \\coerce toSigma (d : D) : \\Sigma Nat Nat
            | con p => p
        \\func f (d : D) : \\Sigma Nat Nat => d
        """);
  }

  @Test
  public void coerceToSigmaProj() {
    typeCheckModule(
      """
        \\data D | con (\\Sigma Nat Nat)
          \\where \\use \\coerce toSigma (d : D) : \\Sigma Nat Nat
            | con p => p
        \\func f (d : D) => d.1
        """);
  }

  @Test
  public void coerceFromUniverse() {
    typeCheckModule(
      """
        \\data D | con \\Type
          \\where \\use \\coerce fromUniverse (X : \\Type) => con X
        \\func f (X : \\Type) : D => X
        """);
  }

  @Test
  public void coerceToUniverse() {
    typeCheckModule(
      """
        \\data D | con \\Type
          \\where \\use \\coerce toUniverse (d : D) : \\Type
            | con X => X
        \\func f (d : D) : \\Type => d
        """);
  }

  @Test
  public void coerceOverriddenField() {
    typeCheckModule(
      """
        \\record R (\\coerce f : \\hType)
        \\record S \\extends R {
          \\override f : \\Set}
        \\func test (s : S) : \\Set => s
        """);
  }

  @Test
  public void transitiveField() {
    typeCheckModule(
      """
        \\class R (X : \\Type)
        \\class S (Y : R)
        \\func test (s : S) (x : s) => x
        """);
  }

  @Test
  public void transitiveField2() {
    typeCheckModule(
      """
        \\class R (X : \\Type)
        \\class S {n : Nat} (Y : \\case n \\with { | 0 => R | suc _ => \\Sigma })
        \\func test (s : S {0}) (x : s) => x
        """);
  }

  @Test
  public void coerceSubclass() {
    typeCheckModule(
      """
        \\record R (n : Nat)
          \\where \\use \\coerce toNat (r : R) => r.n
        \\record S \\extends R
        \\func test (s : S) : Nat => s
        """);
  }

  @Test
  public void finIntTest() {
    typeCheckDef("\\func test (x : Fin 3) : Int => x");
  }
}
