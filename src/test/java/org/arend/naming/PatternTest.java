package org.arend.naming;

import org.arend.Matchers;
import org.junit.Test;

public class PatternTest extends NameResolverTestCase {
  @Test
  public void implicitAvailable() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat
        \\func tests {n : Nat} (m : Nat) : Nat
          | suc m => m
          | zero => n
        """, 1);
  }

  @Test
  public void matchedImplicitAvailable() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat
        \\func tests {n : Nat} (k : Nat) {n : Nat} (m : Nat) : Nat
          | k, suc m => m
          | {_}, k, zero => n
        """, 1);
  }

  @Test
  public void matchedImplicitAvailable2() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat
        \\func tests {n : Nat} (k : Nat) {n : Nat} (m : Nat) : Nat
          | k, suc m => m
          | k, {_}, zero => n
        """, 1);
  }

  @Test
  public void explicitAvailable() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat
        \\func tests {n : Nat} (m : Nat) : Nat
          | {n}, suc m => n
          | {k}, zero => k
        """);
  }

  @Test
  public void explicitNotAvailable() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat
        \\func tests {n : Nat} (m : Nat) : Nat
          | suc _ => m
          | zero => zero
        """, 1);
  }

  @Test
  public void duplicateError() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat
        \\func tests (n m : Nat) : Nat
          | suc n, suc n => zero
          | _, _ => zero
        """, 1);
  }

  @Test
  public void duplicateError2() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat Nat
        \\func tests (n : Nat) : Nat
          | suc n n => zero
          | _ => zero
        """, 1);
  }

  @Test
  public void eliminateOverridden() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat
        \\func tests (n : Nat) (n : Nat) : Nat \\elim n
          | suc _ => zero
          | zero => n
        """);
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

  @Test
  public void longName() {
    resolveNamesModule(
      """
        \\data D | c1 | c2
        \\func f (d : D) : D
          | D.c1 => c1
          | c2 => D.c2
        """);
  }

  @Test
  public void longName2() {
    resolveNamesModule(
      """
        \\func f (n : Nat) : Nat
          | suc (Nat.suc Nat.zero) => 0
          | Nat.zero => 1
          | Nat.suc zero => 2
        """);
  }

  @Test
  public void asPatternTest() {
    resolveNamesDef(
      """
        \\func test (n : Nat) : Nat
          | zero \\as z => z
          | suc n => n
        """);
  }

  @Test
  public void numberTest() {
    resolveNamesDef(
      "\\func test (n : Nat) : Nat\n" +
      "  | 0 x => 0", 1);
  }
}
