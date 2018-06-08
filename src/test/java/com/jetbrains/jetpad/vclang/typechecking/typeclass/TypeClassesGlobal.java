package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

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
  public void inferInstanceSynonym() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\class Y => X\n" +
        "\\instance Nat-X : Y | A => Nat | B => \\lam n => Nat\n" +
        "\\func f => B 0");
  }

  @Test
  public void incorrectInstance() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\func f (n : Nat) : \\oo-Type0 | zero => Nat | suc n => Nat\n" +
        "\\instance Nat-X (n : Nat) : X | A => f n | B => \\lam n => Nat", 1);
  }

  @Test
  public void differentViews() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\class Y => X { B => C }\n" +
        "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat\n" +
        "\\instance Nat-Y : Y | A => Nat | C => \\lam n => Nat -> Nat");
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
        "\\func test : Nat = Nat => path (\\lam _ => f (\\new Y { A => Nat | B => \\lam _ => Nat }))");
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
  public void transitiveMultipleInstances() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\class Y => X { B => C }\n" +
        "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
        "\\instance Nat-Y : Y | A => Nat | C => \\lam n => Nat -> Nat\n" +
        "\\func f {A : \\Type0} {x : X { A => A } } (a : A) => B a\n" +
        "\\func g => f 0");
  }
}
