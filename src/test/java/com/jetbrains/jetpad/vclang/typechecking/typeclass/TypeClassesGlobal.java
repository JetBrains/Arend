package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesGlobal extends TypeCheckingTestCase {
  @Test
  public void inferInstance() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new X' { A => Nat | B => \\lam n => Nat }\n" +
        "\\function f => B 0");
  }

  @Test
  public void inferInstanceRenamed() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\function f => B 0");
  }

  @Test
  public void incorrectInstance() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\function f (n : Nat) : \\Type0 <= \\elim n | zero => Nat | suc n => Nat\n" +
        "\\instance Nat-X (n : Nat) => \\new Y { A => f n | B => \\lam n => Nat }", 1);
  }

  @Test
  public void duplicateInstance() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }", 1);
  }

  @Test
  public void differentViews() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\instance Nat-Z => \\new Z { A => Nat | C => \\lam n => Nat -> Nat }");
  }

  @Test
  public void differentInstances() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\instance I-X => \\new Y { A => I | B => \\lam n => Nat -> Nat }\n" +
        "\\function f => B 0\n" +
        "\\function g => B left");
  }

  @Test
  public void localInstance() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Set0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\function f (y : Y { A => Nat }) => B 0\n" +
        "\\function test : Nat = Nat => path (\\lam _ => f (\\new Y { A => Nat | B => \\lam _ => Nat }))");
  }

  @Test
  public void transitiveInferInstance() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\default \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
        "\\function g => f 0");
  }

  @Test
  public void transitiveInferInstance2() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\default \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\function f {y : Y} (a : y.A) => B a\n" +
        "\\function g => f 0");
  }

  @Test
  public void transitiveNoDefault() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\instance Nat-Z => \\new Z { A => Nat | C => \\lam n => Nat -> Nat }\n" +
        "\\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
        "\\function g => f 0", 2);
  }

  @Test
  public void transitiveDuplicateDefault() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\default \\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\default \\instance Nat-Z => \\new Z { A => Nat | C => \\lam n => Nat -> Nat }\n" +
        "\\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
        "\\function g => f 0", 1);
  }

  @Test
  public void transitiveDefault() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> Nat\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\default \\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => n }\n" +
        "\\instance Nat-Z => \\new Z { A => Nat | C => \\lam n => 0 }\n" +
        "\\function f {A : \\Type0} {z : Z { A => A } } (a : A) => C a\n" +
        "\\function g : f 1 = 1 => path (\\lam _ => 1)");
  }
}
