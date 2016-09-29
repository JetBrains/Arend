package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesNamespaces extends TypeCheckingTestCase {
  @Test
  public void typeClassFullNameInside() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "  \\static \\view \\on X \\by A { B }\n" +
        "}\n" +
        "\\function f (x : M.X) (a : x.A) => M.B a");
  }

  @Test
  public void typeClassFullNameInsideRenamed() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "  \\static \\view Y \\on X \\by A { B }\n" +
        "}\n" +
        "\\function f (x : M.Y) (a : x.A) => M.B a");
  }

  @Test
  public void typeClassFullNameOutside() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\static \\view \\on M.X \\by A { B }\n" +
        "\\function f (x : X) (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameOutsideRenamed() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\static \\view Y \\on M.X \\by A { B }\n" +
        "\\function f (x : Y) (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameInstanceII() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "  \\static \\view \\on X \\by A { B }\n" +
        "  \\static \\instance Nat-X => \\new X { A => Nat | B => \\lam x => x }\n" +
        "}\n" +
        "\\function f => M.B 0", 2);
  }

  @Test
  public void typeClassFullNameInstanceIO() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "  \\static \\view \\on X \\by A { B }\n" +
        "}\n" +
        "\\static \\instance Nat-X => \\new M.X { A => Nat | B => \\lam x => x }\n" +
        "\\function f => M.B 0");
  }

  @Test
  public void typeClassFullNameInstanceOO() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\static \\view \\on M.X \\by A { B }\n" +
        "\\static \\instance Nat-X => \\new X { A => Nat | B => \\lam x => x }\n" +
        "\\function f => B 0");
  }

  @Test
  public void typeClassOpen() {
    typeCheckClass(
        "\\static \\class M {" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\open M\n" +
        "\\static \\view \\on X \\by A { B }\n" +
        "\\function f (x : X) (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameInstanceIIOpen() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "  \\static \\view \\on X \\by A { B }\n" +
        "  \\static \\instance Nat-X => \\new X { A => Nat | B => \\lam x => x }\n" +
        "}\n" +
        "\\open M\n" +
        "\\function f => B 0");
  }
}
