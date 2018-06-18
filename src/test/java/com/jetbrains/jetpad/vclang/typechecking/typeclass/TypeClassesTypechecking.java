package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.error.CycleError;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;

public class TypeClassesTypechecking extends TypeCheckingTestCase {
  @Test
  public void classSynonymExt() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\class X' => X { B => C }\n" +
        "\\func f => \\new X  { A => Nat | B => \\lam _ => Nat }\n" +
        "\\func g => \\new X' { A => Nat | C => \\lam _ => Nat }\n" +
        "\\func p : f = g => path (\\lam _ => f)");
  }

  @Test
  public void notImplementedField() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\class X' => X\n" +
        "\\instance x : X' | A => Nat", 1);
  }

  @Test
  public void mutuallyRecursiveInstance() {
    typeCheckModule(
      "\\instance Nat-X : X | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\func g {x : X { A => Nat }} => \\Prop\n" +
      "\\func f => g\n" +
      "\\instance D-X : X | A => D | B => \\lam _ => f\n" +
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}");
  }

  @Test
  public void mutuallyRecursiveInstanceError() {
    typeCheckModule(
      "\\instance Nat-X : X | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\func g {x : X { A => Nat }} => \\Prop\n" +
      "\\instance D-X : X | A => D | B => \\lam _ => f\n" +
      "\\func f => g\n" +
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}", 1);
    assertThatErrorsAre(instanceOf(CycleError.class));
  }

  @Test
  public void duplicateInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\data D\n" +
      "\\instance D-X : X | A => D | B => \\lam n => D\n" +
      "\\instance D-Y : X | A => D | B => \\lam n => D -> D", 1);
  }
}
