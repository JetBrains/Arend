package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class IntervalTest extends TypeCheckingTestCase {
  @Test
  public void interval1() {
    typeCheckModule(
      "\\func f (n : Nat) (i : I) : Nat\n" +
      "  | zero, _ => 0\n" +
      "  | suc _, _ => 0\n" +
      "  | _, left => 0\n" +
      "  | _, right => 0\n" +
      "\\func g (n : Nat) : f n left = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void intervalRedundantClause() {
    typeCheckModule(
      "\\func f (n : Nat) (i : I) : Nat\n" +
      "  | zero, _ => 0\n" +
      "  | suc _, _ => 0\n" +
      "  | _, left => 0\n" +
      "  | _, right => 0\n" +
      "  | _, _ => 0\n" +
      "\\func g (n : Nat) : f n left = 0 => path (\\lam _ => 0)", 1);
  }

  @Test
  public void interval1error() {
    typeCheckModule(
      "\\func f (n : Nat) (i : I) : Nat\n" +
      "  | zero, _ => 0\n" +
      "  | suc _, _ => 1\n" +
      "  | _, left => 0\n" +
      "  | _, right => 0", 2);
  }

  @Test
  public void interval2error() {
    typeCheckModule(
      "\\func f (n : Nat) (i j : I) : Nat\n" +
      "  | zero, _, _ => 0\n" +
      "  | suc _, _, _ => 0\n" +
      "  | _, left, _ => 0\n" +
      "  | _, _, right => 0\n" +
      "\\func g (n : Nat) (k : I) : f n left k = f n k right => path (\\lam _ => 0)", 1);
  }

  @Test
  public void intervalEvalError() {
    typeCheckModule(
      "\\func f (n : Nat) (i : I) : Nat\n" +
      "  | zero, _ => 0\n" +
      "  | suc _, _ => 0\n" +
      "  | _, left => 0\n" +
      "  | _, right => 0\n" +
      "\\func g (n : Nat) (k : I) : f n k = 0 => path (\\lam _ => 0)", 1);
  }

  @Test
  public void intervalCoverageError() {
    typeCheckModule(
      "\\func f (i : I) : Nat\n" +
      "  | left => 0\n" +
      "  | right => 0", 1);
  }

  @Test
  public void intervalCoverageError2() {
    typeCheckModule(
      "\\func f (n : Nat) (i : I) : Nat\n" +
      "  | zero, _ => 0\n" +
      "  | _, left => 0\n" +
      "  | _, right => 0", 1);
  }

  @Test
  public void at() {
    typeCheckModule(
      "\\func at {A : I -> \\Type} {a : A left} {a' : A right} (p : Path A a a') (i : I) : A i \\elim p, i\n" +
      "  | _, left => a\n" +
      "  | _, right => a'\n" +
      "  | path f, i => f i\n" +
      "\\func g (p : 0 = 1) : at p right = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void atConditionsError() {
    typeCheckModule(
      "\\func at {A : \\Type} (a a' : A) (p : a = a') (i : I) : A \\elim p, i\n" +
      "  | path f, i => f i\n" +
      "  | _, left => a'\n" +
      "  | _, right => a", 2);
  }

  @Test
  public void twoPatterns() {
    typeCheckModule(
      "\\func f (n m : Nat) (i : I) : Nat\n" +
      "  | zero, _, _ => 0\n" +
      "  | _, zero, _ => 0\n" +
      "  | _, suc _, _ => 0\n" +
      "  | _, _, left => 0\n" +
      "  | _, _, right => 0\n" +
      "\\func g (n : Nat) (i : I) : f zero n i = 0 => path (\\lam _ => 0)", 1);
  }

  @Test
  public void twoPatternsError() {
    typeCheckModule(
      "\\func f (n m : Nat) (i : I) : Nat\n" +
      "  | zero, _, _ => 0\n" +
      "  | _, zero, _ => 0\n" +
      "  | _, suc _, _ => 0\n" +
      "  | _, _, left => 0\n" +
      "  | _, _, right => 0\n" +
      "\\func g (n : Nat) (i : I) : f n zero i = 0 => path (\\lam _ => 0)", 1);
  }

  @Test
  public void testElimOnInterval() {
    typeCheckDef(
      "\\func test (i : I) : Nat\n" +
      "  | left => 0\n" +
      "  | right => 0\n"  +
      "  | _ => 0");
  }

  @Test
  public void testElimOnIntervalError() {
    typeCheckDef(
      "\\func test (i : I) : Nat\n" +
      "  | left => 0\n" +
      "  | right => 0\n"  +
      "  | _ => 1", 2);
  }
}
