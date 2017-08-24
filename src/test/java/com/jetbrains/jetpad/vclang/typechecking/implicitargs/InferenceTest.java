package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.goal;

public class InferenceTest extends TypeCheckingTestCase {
  @Test
  public void doubleGoalTest() {
    String text =
        "\\data B | t | f\n" +
        "\\data D Nat | con Nat\n" +
        "\\function h : D 0 => con (\\case t \\with {\n" +
        "  | t => {?}\n" +
        "  | f => {?}\n" +
        "})";
    typeCheckModule(text, 2);
    assertThatErrorsAre(goal(0), goal(0));
  }

  @Test
  public void inferTailConstructor1() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function f : D 0 {1} 2 => con");
  }

  @Test
  public void inferTailConstructor2() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function f : \\Pi {m : Nat} -> D 0 {1} m => con");
  }

  @Test
  public void inferTailConstructor3() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function f : \\Pi {k m : Nat} -> D 0 {k} m => con");
  }

  @Test
  public void inferTailConstructor4() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function f : \\Pi {n k m : Nat} -> D n {k} m => con");
  }

  @Test
  public void inferConstructor1a() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function f => con {0} (path (\\lam _ => 1))");
  }

  @Test
  public void inferConstructor1b() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = m)\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f => con {0} idp idp");
  }

  @Test
  public void inferConstructor2a() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function f => (D 0).con (path (\\lam _ => 1))");
  }

  @Test
  public void inferConstructor2b() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = m)\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f => (D 0).con idp idp");
  }

  @Test
  public void inferConstructor3() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = n)\n" +
        "\\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\function f => con {0} idp idp", 1);
  }

  @Test
  public void equations() {
    typeCheckModule(
        "\\data E (A B : \\Type) | inl A | inr B\n" +
        "\\data Empty : \\Prop\n" +
        "\\function neg (A : \\Type) => A -> Empty\n" +
        "\\function test (A : \\Type) => E (neg A) A"
    );
  }
}
