package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesNamespaces extends TypeCheckingTestCase {
  @Test
  public void typeClassFullNameInside() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "}\n" +
        "\\function f (x : M.X') (a : x.A) => M.B a");
  }

  @Test
  public void typeClassFullNameInsideRenamed() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view Y \\on X \\by A { B }\n" +
        "}\n" +
        "\\function f (x : M.Y) (a : x.A) => M.B a");
  }

  @Test
  public void typeClassFullNameOutside() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\view X' \\on M.X \\by A { B }\n" +
        "\\function f (x : X') (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameOutsideRenamed() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\view Y \\on M.X \\by A { B }\n" +
        "\\function f (x : Y) (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameInstanceInside() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "  \\instance Nat-X => \\new X' { A => Nat | B => \\lam x => x }\n" +
        "  \\function T => B 0 = 0\n" +
        "}\n" +
        "\\function f (t : M.T) => 0");
  }

  @Test
  public void typeClassFullNameInstanceII() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "  \\instance Nat-X => \\new X' { A => Nat | B => \\lam x => x }\n" +
        "  \\function T => B 0 = 0\n" +
        "}\n" +
        "\\function f (t : M.T) => M.B 0", 1);
  }

  @Test
  public void typeClassFullNameInstanceIO() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "}\n" +
        "\\instance Nat-X => \\new M.X' { A => Nat | B => \\lam x => x }\n" +
        "\\function f => M.B 0");
  }

  @Test
  public void typeClassFullNameInstanceOO() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\view X' \\on M.X \\by A { B }\n" +
        "\\instance Nat-X => \\new X' { A => Nat | B => \\lam x => x }\n" +
        "\\function f => B 0");
  }

  @Test
  public void typeClassOpen() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\open M\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (x : X') (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameInstanceIIOpen() {
    typeCheckClass(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "  \\instance Nat-X => \\new X' { A => Nat | B => \\lam x => x }\n" +
        "}\n" +
        "\\open M\n" +
        "\\function f => B 0");
  }
}
