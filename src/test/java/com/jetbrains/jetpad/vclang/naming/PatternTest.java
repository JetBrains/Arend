package com.jetbrains.jetpad.vclang.naming;

import org.junit.Test;

public class PatternTest extends NameResolverTestCase {
  @Test
  public void implicitAvailable() {
    resolveNamesClass(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | suc m => m\n" +
      "  | zero => n", 1);
  }

  @Test
  public void matchedImplicitAvailable() {
    resolveNamesClass(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (k : Nat) {n : Nat} (m : Nat) : Nat\n" +
      "  | k, suc m => m\n" +
      "  | {_}, k, zero => n", 1);
  }

  @Test
  public void matchedImplicitAvailable2() {
    resolveNamesClass(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (k : Nat) {n : Nat} (m : Nat) : Nat\n" +
      "  | k, suc m => m\n" +
      "  | k, {_}, zero => n", 1);
  }

  @Test
  public void explicitAvailable() {
    resolveNamesClass(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | {n}, suc m => n\n" +
      "  | {k}, zero => k");
  }

  @Test
  public void explicitNotAvailable() {
    resolveNamesClass(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | suc _ => m\n" +
      "  | zero => zero", 1);
  }

  @Test
  public void duplicateError() {
    resolveNamesClass(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests (n m : Nat) : Nat\n" +
      "  | suc n, suc n => zero\n" +
      "  | _, _ => zero", 1);
  }

  @Test
  public void duplicateError2() {
    resolveNamesClass(
      "\\data Nat | zero | suc Nat Nat\n" +
      "\\function tests (n : Nat) : Nat\n" +
      "  | suc n n => zero\n" +
      "  | _ => zero", 1);
  }

  @Test
  public void eliminateOverridden() {
    resolveNamesClass(
      "\\data Nat | zero | suc Nat\n" +
      "\\function tests (n : Nat) (n : Nat) : Nat => \\elim n\n" +
      "  | suc _ => zero\n" +
      "  | zero => n");
  }
}
