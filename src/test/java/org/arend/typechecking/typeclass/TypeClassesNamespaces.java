package org.arend.typechecking.typeclass;

import org.arend.typechecking.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.core.expr.ExpressionFactory.Nat;

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
    assertThatErrorsAre(Matchers.instanceInference(get("M.X"), Nat()));
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
      "  \\func f : n = n => path (\\lam _ => 0)\n" +
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
