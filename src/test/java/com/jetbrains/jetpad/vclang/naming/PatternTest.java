package com.jetbrains.jetpad.vclang.naming;

import org.junit.Test;

public class PatternTest extends NameResolverTestCase {
  @Test
  public void implicitAvailable() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | suc m => m\n" +
      "  | zero => n", 1);
  }

  @Test
  public void matchedImplicitAvailable() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (k : Nat) {n : Nat} (m : Nat) : Nat\n" +
      "  | k, suc m => m\n" +
      "  | {_}, k, zero => n", 1);
  }

  @Test
  public void matchedImplicitAvailable2() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (k : Nat) {n : Nat} (m : Nat) : Nat\n" +
      "  | k, suc m => m\n" +
      "  | k, {_}, zero => n", 1);
  }

  @Test
  public void explicitAvailable() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | {n}, suc m => n\n" +
      "  | {k}, zero => k");
  }

  @Test
  public void explicitNotAvailable() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | suc _ => m\n" +
      "  | zero => zero", 1);
  }

  @Test
  public void duplicateError() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests (n m : Nat) : Nat\n" +
      "  | suc n, suc n => zero\n" +
      "  | _, _ => zero", 1);
  }

  @Test
  public void duplicateError2() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat Nat\n" +
      "\\function tests (n : Nat) : Nat\n" +
      "  | suc n n => zero\n" +
      "  | _ => zero", 1);
  }

  @Test
  public void eliminateOverridden() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests (n : Nat) (n : Nat) : Nat => \\elim n\n" +
      "  | suc _ => zero\n" +
      "  | zero => n");
  }

  @Test
  public void patternWrongDefinition() {
    resolveNamesModule(
        "\\data Nat | zero | suc Nat\n" +
        "\\data D (n m : Nat) | d\n" +
        "\\data C | c (n m : Nat) (D n m)\n" +
        "\\data E C \\with | E (c zero (suc zero) d) => e", 1);
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
      "\\function test (x : Nat) : Nat | zero a => 0 | sucs n => 1", 1);
  }

  @Test
  public void functionPatternWrongDefinition() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function test (x : Nat) : Nat | zero a => 0 | Nat => 1", 1);
  }

  @Test
  public void elimPatternUnknownConstructor() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function test (x : Nat) : Nat => \\elim x | zero a => 0 | sucs n => 1", 1);
  }

  @Test
  public void elimPatternWrongDefinition() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function test (x : Nat) : Nat => \\elim x | zero a => 0 | Nat => 1", 1);
  }

  @Test
  public void casePatternUnknownConstructor() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function test (x : Nat) : Nat => \\case x \\with { zero a => 0 | sucs n => 1 }", 1);
  }

  @Test
  public void casePatternWrongDefinition() {
    resolveNamesModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function test (x : Nat) : Nat => \\case x \\with { zero a => 0 | Nat => 1 }", 1);
  }
}
