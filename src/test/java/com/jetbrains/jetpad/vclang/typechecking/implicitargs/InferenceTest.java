package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class InferenceTest extends TypeCheckingTestCase {
  @Test
  public void doubleGoalTest() {
    String text =
        "\\static \\data B | t | f\n" +
        "\\static \\data D Nat | con Nat\n" +
        "\\static \\function h : D 0 => con (\\case t\n" +
        "  | t => {?}\n" +
        "  | f => {?})";
    typeCheckClass(text, 0, 2);
    assertThatErrorsAre(goal(0), goal(0));
  }

  @Test
  public void inferTailConstructor1() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function f : D 0 {1} 2 => con");
  }

  @Test
  public void inferTailConstructor2() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function f : \\Pi {m : Nat} -> D 0 {1} m => con");
  }

  @Test
  public void inferTailConstructor3() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function f : \\Pi {k m : Nat} -> D 0 {k} m => con");
  }

  @Test
  public void inferTailConstructor4() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function f : \\Pi {n k m : Nat} -> D n {k} m => con");
  }

  @Test
  public void inferConstructor1a() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\static \\function f => con {0} (path (\\lam _ => 1))");
  }

  @Test
  public void inferConstructor1b() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = m)\n" +
        "\\static \\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp ,lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f => con {0} idp idp");
  }

  @Test
  public void inferConstructor2a() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\static \\function f => (D 0).con (path (\\lam _ => 1))");
  }

  @Test
  public void inferConstructor2b() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = m)\n" +
        "\\static \\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f => (D 0).con idp idp");
  }

  @Test
  public void inferConstructor3() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = n)\n" +
        "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f => con {0} idp idp", 1);
  }

  @Test
  public void equations() {
    typeCheckClass(
        "\\static \\data E (A B : \\Type) | inl A | inr B\n" +
        "\\static \\data Empty : \\Prop\n" +
        "\\static \\function neg (A : \\Type) => A -> Empty\n" +
        "\\static \\function test (A : \\Type) => E (neg A) A"
    );
  }
}
