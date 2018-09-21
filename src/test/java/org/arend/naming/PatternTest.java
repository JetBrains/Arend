package org.arend.naming;

import org.arend.typechecking.Matchers;
import org.junit.Test;

public class PatternTest extends NameResolverTestCase {
  @Test
  public void implicitAvailable() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func tests {n : Nat} (m : Nat) : Nat\n" +
      "  | suc m => m\n" +
      "  | zero => n", 1);
  }

  @Test
  public void matchedImplicitAvailable() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func tests {n : Nat} (k : Nat) {n : Nat} (m : Nat) : Nat\n" +
      "  | k, suc m => m\n" +
      "  | {_}, k, zero => n", 1);
  }

  @Test
  public void matchedImplicitAvailable2() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func tests {n : Nat} (k : Nat) {n : Nat} (m : Nat) : Nat\n" +
      "  | k, suc m => m\n" +
      "  | k, {_}, zero => n", 1);
  }

  @Test
  public void explicitAvailable() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func tests {n : Nat} (m : Nat) : Nat\n" +
      "  | {n}, suc m => n\n" +
      "  | {k}, zero => k");
  }

  @Test
  public void explicitNotAvailable() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func tests {n : Nat} (m : Nat) : Nat\n" +
      "  | suc _ => m\n" +
      "  | zero => zero", 1);
  }

  @Test
  public void duplicateError() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func tests (n m : Nat) : Nat\n" +
      "  | suc n, suc n => zero\n" +
      "  | _, _ => zero", 1);
  }

  @Test
  public void duplicateError2() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat Nat\n" +
      "\\func tests (n : Nat) : Nat\n" +
      "  | suc n n => zero\n" +
      "  | _ => zero", 1);
  }

  @Test
  public void eliminateOverridden() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func tests (n : Nat) (n : Nat) : Nat \\elim n\n" +
      "  | suc _ => zero\n" +
      "  | zero => n");
  }

  @Test
  public void patternUnknownConstructor() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\data D Nat \\with | suc (luc m) => d", 1);
  }

  @Test
  public void functionPatternUnknownConstructor() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func test (x : Nat) : Nat | zero a => 0 | sucs n => 1", 1);
  }

  @Test
  public void elimPatternUnknownConstructor() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func test (x : Nat) : Nat | zero a => 0 | sucs n => 1", 1);
  }

  @Test
  public void casePatternUnknownConstructor() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func test (x : Nat) : Nat => \\case x \\with { zero a => 0 | sucs n => 1 }", 1);
  }

  @Test
  public void freeVars() {
    resolveNamesModule(
      "\\data D | c1 \\Prop | c2 \\Prop\n" +
      "\\func f (d : D) : \\Prop => \\case d \\with { c1 x => x | c2 y => x }", 1);
    assertThatErrorsAre(Matchers.notInScope("x"));
  }
}
