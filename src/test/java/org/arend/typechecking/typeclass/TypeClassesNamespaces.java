package org.arend.typechecking.typeclass;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.instanceInference;

public class TypeClassesNamespaces extends TypeCheckingTestCase {
  @Test
  public void typeClassFullNameInside() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\func f (x : M.X) (a : x.A) => M.B a");
  }

  @Test
  public void typeClassFullNameInsideSynonym() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\class Y => X\n" +
        "  \\func g {x : X} (a : x.A) => B a" +
        "}\n" +
        "\\func f (x : M.Y) (a : x.A) => M.g a");
  }

  @Test
  public void typeClassFullNameOutsideSynonym() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\func g {x : X} (a : x.A) => B a" +
        "}\n" +
        "\\class Y => M.X\n" +
        "\\func f (y : Y) (a : y.A) => M.g a");
  }

  @Test
  public void typeClassFullNameInstanceInside() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\instance Nat-X : X | A => Nat | B => \\lam x => x\n" +
        "  \\func T => B 0 = 0\n" +
        "}\n" +
        "\\func f (t : M.T) => 0");
  }

  @Test
  public void typeClassFullNameInstanceII() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\instance Nat-X : X | A => Nat | B => \\lam x => x\n" +
        "  \\func T => B 0 = 0\n" +
        "}\n" +
        "\\func f (t : M.T) => M.B 0", 1);
    assertThatErrorsAre(instanceInference(getDefinition("M.X")));
  }

  @Test
  public void typeClassFullNameInstanceIO() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\class Y => X\n" +
        "  \\func g {x : X} (a : x.A) => B a" +
        "}\n" +
        "\\instance Nat-X : M.Y | A => Nat | B => \\lam x => x\n" +
        "\\func f => M.g 0");
  }

  @Test
  public void typeClassFullNameInstanceOO() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\func g {x : X} (a : x.A) => B a" +
        "}\n" +
        "\\class Y => M.X\n" +
        "\\instance Nat-X : Y | A => Nat | B => \\lam x => x\n" +
        "\\func f => M.g 0");
  }

  @Test
  public void typeClassOpen() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\open M\n" +
        "\\func f (x : X) (a : x.A) => B a");
  }

  @Test
  public void typeClassFullNameInstanceIIOpen() {
    typeCheckModule(
        "\\class M \\where {\n" +
        "  \\class X (A : \\Type0) {\n" +
        "    | B : A -> Nat\n" +
        "  }\n" +
        "  \\instance Nat-X : X | A => Nat | B => \\lam x => x\n" +
        "}\n" +
        "\\open M\n" +
        "\\func f => B 0");
  }

  @Test
  public void overriddenInstance() {
    typeCheckModule(
      "\\class A { | n : Nat }\n" +
      "\\instance a0 : A | n => 0\n" +
      "\\class M \\where {\n" +
      "  \\instance a1 : A | n => 1\n" +
      "  \\func f : n = n => path (\\lam _ => 1)\n" +
      "}");
  }

  @Test
  public void dotReference() {
    typeCheckModule(
      "\\class X (A : \\Type) | xxx : A\n" +
      "\\func foo => NatX.xxx\n" +
      "\\instance NatX : X Nat | xxx => 0");
  }
}
