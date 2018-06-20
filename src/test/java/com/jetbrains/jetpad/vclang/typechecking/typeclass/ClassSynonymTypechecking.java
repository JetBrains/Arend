package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class ClassSynonymTypechecking extends TypeCheckingTestCase {
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

  @Test
  public void inferVarSynonym() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X\n" +
      "\\func f (y : Y) (a : y.A) => B a");
  }

  @Test
  public void inferVarSynonym2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X\n" +
      "\\func f (A' : \\Type0) (y : Y { A => A' }) (a : A') => B a");
  }

  @Test
  public void inferVarDifferent() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f (A' : \\Type0) (x : X { A => A' }) {y : Y { A => A' } } (a : A') => B a = C a");
  }

  @Test
  public void transitiveInferLocal() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f {A : \\Type0} {x : X { A => A } } (a : A) => B a\n" +
      "\\func g (y : Y) (a : y.A) => f a");
  }

  @Test
  public void transitiveInferLocal2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f {x : X} (a : x.A) => B a\n" +
      "\\func g (y : Y) (a : y.A) => f a");
  }

  @Test
  public void transitiveLocalDuplicate() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f {A : \\Type0} {x : X { A => A }} (a : A) => B a\n" +
      "\\func g {A : \\Type0} {x : X { A => A }} {y : Y { A => A }} (a : A) : f a = y.B a => path (\\lam _ => B a)");
  }

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
}
