package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesGlobal extends TypeCheckingTestCase {
  @Test
  public void inferInstance() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new X' { A => Nat | B => \\lam n => Nat }\n" +
        "\\function f => B 0");
  }

  @Test
  public void inferInstanceRenamed() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\function f => B 0");
  }

  @Test
  public void incorrectInstance() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\function f (n : Nat) : \\oo-Type0 => \\elim n | zero => Nat | suc n => Nat\n" +
        "\\instance Nat-X (n : Nat) => \\new Y { A => f n | B => \\lam n => Nat }", 1);
  }

  @Test
  public void differentViews() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\instance Nat-Z => \\new Z { A => Nat | C => \\lam n => Nat -> Nat }");
  }

  @Test
  public void differentInstances() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\instance I-X => \\new Y { A => I | B => \\lam n => Nat -> Nat }\n" +
        "\\function f => B 0\n" +
        "\\function g => B left");
  }

  @Test
  public void localInstance() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Set0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\function f (y : Y { A => Nat }) => B 0\n" +
        "\\function test : Nat = Nat => path (\\lam _ => f (\\new Y { A => Nat | B => \\lam _ => Nat }))");
  }

  @Test
  public void transitiveInferInstance() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\default \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
        "\\function g => f 0");
  }

  @Test
  public void transitiveInferInstance2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\default \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\function f {y : Y} (a : y.A) => B a\n" +
        "\\function g => f 0");
  }

  @Test
  public void transitiveNoDefault() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\instance Nat-Z => \\new Z { A => Nat | C => \\lam n => Nat -> Nat }\n" +
        "\\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
        "\\function g => f 0", 1);
  }

  @Test
  public void transitiveDefault() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\default \\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => n }\n" +
        "\\instance Nat-Z => \\new Z { A => Nat | C => \\lam n => 0 }\n" +
        "\\function f {A : \\Type0} {z : Z { A => A } } (a : A) => C a\n" +
        "\\function g : f 1 = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void twoDefaults() {
    typeCheckModule(
        "\\class X1 {\n" +
        "  | A : \\Type0\n" +
        "  | B1 : A -> Nat\n" +
        "}\n" +
        "\\class X2 {\n" +
        "  | A : \\Type0\n" +
        "  | B2 : A -> Nat\n" +
        "}\n" +
        "\\view Y1 \\on X1 \\by A { B1 }\n" +
        "\\view Y2 \\on X2 \\by A { B2 }\n" +
        "\\default \\instance Nat-Y1 => \\new Y1 { A => Nat | B1 => \\lam n => n }\n" +
        "\\default \\instance Nat-Y2 => \\new Y2 { A => Nat | B2 => \\lam n => 0 }\n" +
        "\\function f {A : \\Type0} {z : Y1 { A => A } } (a : A) => B1 a\n" +
        "\\function g : f 1 = 1 => path (\\lam _ => 1)");
  }
}
