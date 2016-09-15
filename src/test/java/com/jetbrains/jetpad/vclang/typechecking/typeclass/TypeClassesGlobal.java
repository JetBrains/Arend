package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesGlobal extends TypeCheckingTestCase {
  @Test
  public void inferInstance() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { B }\n" +
        "\\static \\instance Nat-X => \\new X { A => Nat | B => \\lam n => Nat }\n" +
        "\\static \\function f => B 0");
  }

  @Test
  public void inferInstanceRenamed() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\static \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\static \\function f => B 0");
  }

  @Test
  public void instanceWithoutView() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0\n" +
        "}\n" +
        "\\static \\instance Nat-X => \\new X { A => Nat | B => \\lam n => Nat }", 1);
  }

  @Test
  public void incorrectInstance() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\static \\function f (n : Nat) : \\Type0 <= \\elim n | zero => Nat | suc n => Nat\n" +
        "\\static \\instance Nat-X (n : Nat) => \\new Y { A => f n | B => \\lam n => Nat }", 1);
  }

  @Test
  public void duplicateInstance() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\static \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\static \\instance Nat-Y => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n", 1);
  }

  @Test
  public void differentInstances() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\static \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }\n" +
        "\\static \\instance I-X => \\new Y { A => I | B => \\lam n => Nat -> Nat }\n" +
        "\\static \\function f => B 0\n" +
        "\\static \\function g => B left");
  }

  @Test
  public void localInstance() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\static \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\static \\function f (y : Y { A => Nat }) => B 0\n" +
        "\\static \\function test : Nat = Nat => path (\\lam _ => f (\\new Y { A => Nat | B => \\lam _ => Nat }))");
  }

  @Test
  public void transitiveInferInstance() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\static \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat -> Nat }\n" +
        "\\static \\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
        "\\static \\function g => f 0");
  }
}
