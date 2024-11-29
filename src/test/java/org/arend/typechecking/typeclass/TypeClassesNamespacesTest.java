package org.arend.typechecking.typeclass;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.core.expr.ExpressionFactory.Nat;

public class TypeClassesNamespacesTest extends TypeCheckingTestCase {
  @Test
  public void typeClassFullNameInside() {
    typeCheckModule("""
      \\class M \\where {
        \\class X (A : \\Type0) {
          | B : A -> Nat
        }
      }
      \\func f (x : M.X) (a : M.X.A {x}) => M.B a
      """);
  }

  @Test
  public void typeClassFullNameInstanceInside() {
    typeCheckModule("""
      \\class M \\where {
        \\class X (A : \\Type0) {
          | B : A -> Nat
        }
        \\instance Nat-X : X | A => Nat | B => \\lam x => x
        \\func T => B 0 = 0
      }
      \\func f (t : M.T) => 0
      """);
  }

  @Test
  public void typeClassFullNameInstanceII() {
    typeCheckModule("""
      \\class M \\where {
        \\class X (A : \\Type0) {
          | B : A -> Nat
        }
        \\instance Nat-X : X | A => Nat | B => \\lam x => x
        \\func T => B 0 = 0
      }
      \\func f (t : M.T) => M.B 0
      """, 1);
    assertThatErrorsAre(Matchers.instanceInference(get("M.X"), Nat()));
  }

  @Test
  public void typeClassOpen() {
    typeCheckModule( """
      \\class M \\where {
        \\class X (A : \\Type0) {
          | B : A -> Nat
        }
      }
      \\open M
      \\func f (x : X) (a : x.A) => B a
      """);
  }

  @Test
  public void typeClassFullNameInstanceIIOpen() {
    typeCheckModule( """
      \\class M \\where {
        \\class X (A : \\Type0) {
          | B : A -> Nat
        }
        \\instance Nat-X : X | A => Nat | B => \\lam x => x
      }
      \\open M
      \\func f => B 0
      """);
  }

  @Test
  public void overriddenInstance() {
    typeCheckModule( """
      \\class A { | n : Nat }
      \\instance a0 : A | n => 0
      \\class M \\where {
        \\instance a1 : A | n => 1
        \\func f : n = n => path (\\lam _ => 1)
      }
      """);
  }

  @Test
  public void overriddenFieldInstance() {
    typeCheckModule( """
      \\class A { | n : Nat }
      \\class M (a0 : A) {
        \\instance a1 : A | n => 1
        \\func f : n = n => path (\\lam _ => 1)
      }
      """);
  }

  @Test
  public void dotReference() {
    typeCheckModule( """
      \\class X (A : \\Type) | xxx : A
      \\func foo => NatX.xxx
      \\instance NatX : X Nat | xxx => 0
      """);
  }
}
