package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesNamespaces extends TypeCheckingTestCase {
  @Test
  public void typeClassFullNameInside() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "}\n" +
        "\\func f (x : M.X') (a : x.A) => M.B a");
  }

  @Test
  public void typeClassFullNameInsideRenamed() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view Y \\on X \\by A { B }\n" +
        "}\n" +
        "\\func f (x : M.Y) (a : x.A) => M.B a");
  }

  @Test
  public void typeClassFullNameOutside() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\view X' \\on M.X \\by A { B }\n" +
        "\\func f (x : X') (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameOutsideRenamed() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\view Y \\on M.X \\by A { B }\n" +
        "\\func f (x : Y) (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameInstanceInside() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "  \\instance Nat-X : X' | A => Nat | B => \\lam x => x\n" +
        "  \\func T => B 0 = 0\n" +
        "}\n" +
        "\\func f (t : M.T) => 0");
  }

  @Test
  public void typeClassFullNameInstanceII() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "  \\instance Nat-X : X' | A => Nat | B => \\lam x => x\n" +
        "  \\func T => B 0 = 0\n" +
        "}\n" +
        "\\func f (t : M.T) => M.B 0", 1);
  }

  @Test
  public void typeClassFullNameInstanceIO() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "}\n" +
        "\\instance Nat-X : M.X' { A => Nat | B => \\lam x => x\n" +
        "\\func f => M.B 0");
  }

  @Test
  public void typeClassFullNameInstanceOO() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\view X' \\on M.X \\by A { B }\n" +
        "\\instance Nat-X : X' | A => Nat | B => \\lam x => x\n" +
        "\\func f => B 0");
  }

  @Test
  public void typeClassOpen() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\open M\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\func f (x : X') (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameInstanceIIOpen() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X {\n" +
        "    | A : \\Type0\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\view X' \\on X \\by A { B }\n" +
        "  \\instance Nat-X : X' | A => Nat | B => \\lam x => x\n" +
        "}\n" +
        "\\open M\n" +
        "\\func f => B 0");
  }
}
