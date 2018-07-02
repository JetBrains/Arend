package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.error.CycleError;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.duplicateInstanceError;
import static com.jetbrains.jetpad.vclang.typechecking.Matchers.typeMismatchError;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class TypeClassesGlobal extends TypeCheckingTestCase {
  @Test
  public void inferInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat\n" +
      "\\func f => B 0");
  }

  @Test
  public void instanceClassWithArg() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X Nat | B => \\lam n => Nat\n" +
      "\\func f => B 0");
  }

  @Test
  public void incorrectInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\func f : \\Type1 => Nat\n" +
      "\\instance Nat-X : X | A => f | B => \\lam n => Nat", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void differentInstances() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat\n" +
      "\\instance I-X : X | A => I | B => \\lam n => Nat -> Nat\n" +
      "\\func f => B 0\n" +
      "\\func g => B left");
  }

  @Test
  public void localInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Set0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f (y : X { A => Nat }) => B 0\n" +
      "\\func test : Nat = Nat => path (\\lam _ => f (\\new X { A => Nat | B => \\lam _ => Nat }))");
  }

  @Test
  public void transitiveInferInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f {A : \\Type0} {y : X { A => A } } (a : A) => B a\n" +
      "\\func g => f 0");
  }

  @Test
  public void transitiveInferInstance2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f {x : X} (a : x.A) => B a\n" +
      "\\func g => f 0");
  }

  @Test
  public void mutuallyRecursiveInstance() {
    typeCheckModule(
      "\\instance Nat-X : X | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\func g {x : X { A => Nat }} => \\Prop\n" +
      "\\func f : \\Set0 => g\n" +
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
      "\\func f : \\Set0 => g\n" +
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}", 1);
    assertThatErrorsAre(instanceOf(CycleError.class));
  }

  // We do not check for duplicate global instances currently
  @Test
  public void duplicateInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\data D\n" +
      "\\instance D-X : X | A => D | B => \\lam n => D\n" +
      "\\instance D-Y : X | A => D | B => \\lam n => D -> D");
    // assertThatErrorsAre(duplicateInstanceError());
  }
}
