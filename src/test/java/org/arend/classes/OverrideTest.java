package org.arend.classes;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.arend.Matchers.fieldsImplementation;
import static org.arend.Matchers.typeMismatchError;

public class OverrideTest extends TypeCheckingTestCase {
  @Test
  public void subtypeTest() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A | a => 0
      \\class C | f : A
      \\class D \\extends C { \\override f : B }
      \\func test (d : D) : d.f.a = 0 => idp
      """);
  }

  @Test
  public void subtypeError() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat)
      \\class C | f : A
      \\class D \\extends C { \\override f : B }
      """, 1);
  }

  @Test
  public void resolveError() {
    resolveNamesModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class C | g : A
      \\class D \\extends C { \\override f : B }
      """, 1);
  }

  @Test
  public void overrideOwnFieldError() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class C | g : A
      \\class D \\extends C { | f : A \\override f : B }
      """, 1);
  }

  @Test
  public void propertyTest() {
    typeCheckModule("""
      \\class A
      \\class B \\extends A
      \\class C { \\property f : A }
      \\class D \\extends C { \\override f : B }
      """);
  }

  @Test
  public void propertyFieldError() {
    typeCheckModule("""
      \\class A
      \\class B (b : Nat) \\extends A
      \\class C { \\property f : A }
      \\class D \\extends C { \\override f : B }
      """, 1);
  }

  @Test
  public void extensionTest() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class B' (b' : Nat) \\extends A
      \\class L \\extends B, B' | a => 0
      \\class C | f : A
      \\class D \\extends C { \\override f : B }
      \\class D' \\extends C { \\override f : B' }
      \\class K \\extends D, D' { \\override f : L }
      \\func test (k : K) : k.f.a = 0 => idp
      """);
  }

  @Test
  public void extensionError() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class L \\extends A | a => 0
      \\class C | f : A
      \\class D \\extends C { \\override f : B }
      \\class E \\extends D
      \\class F \\extends E { \\override f : L }
      """, 1);
  }

  @Test
  public void extensionConflictTest() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class C | f : A
      \\class D \\extends C { \\override f : B }
      \\class D' \\extends C { \\override f : B }
      \\class E \\extends D, D'
      """);
  }

  @Test
  public void extensionConflictError() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class B' (b' : Nat) \\extends A
      \\class C | f : A
      \\class D \\extends C { \\override f : B }
      \\class D' \\extends C { \\override f : B' }
      \\class E \\extends D, D'
      """, 1);
  }

  @Test
  public void overriddenFieldTypeTest() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class C | f : A
      \\class D \\extends C {
        \\override f : B
      }
      \\func test (d : D) : B => d.f
      """);
  }

  @Test
  public void dependentFieldsTest() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class C | f : A
      \\class D \\extends C {
        \\override f : B
        | g : f.a = B.b {f}
      }
      """);
  }

  @Test
  public void dependentFieldsError() {
    typeCheckModule("""
      \\class A (a : Nat)
      \\class B (b : Nat) \\extends A
      \\class C | f : A
      \\class D \\extends C {
        | g : f.a = B.b {f}
        \\override f : B
      }
      """, 1);
  }

  @Test
  public void newTest() {
    typeCheckModule("""
      \\record R
      \\record S \\extends R
      \\record A | foo : R
      \\record B \\extends A {
        \\override foo : S
      }
      \\func test => B (\\new R)
      """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void implementTest() {
    typeCheckModule("""
      \\record R
      \\record S \\extends R
      \\record S' \\extends R
      \\record A | foo : R
      \\record B \\extends A {
        \\override foo : S
      }
      \\record C \\extends B
        | foo => \\new R
      """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void derivedOverridePi() {
    typeCheckModule("""
      \\record R (X : \\Set (\\suc \\lp)) (f : X -> X)
      \\record S \\extends R {
        \\override X : \\Set \\lp
      }
      \\func test (A : \\Set \\lp) : \\Set \\lp => S \\lp A
      """);
  }

  @Test
  public void derivedOverrideSigma() {
    typeCheckModule("""
      \\record R (X : \\Set (\\suc \\lp)) (f : \\Sigma X X)
      \\record S \\extends R {
        \\override X : \\Set \\lp
      }
      \\func test (A : \\Set \\lp) : \\Set \\lp => S \\lp A
      """);
  }

  @Test
  public void overrideImplementedError() {
    typeCheckModule("""
      \\record M (x : Nat)
      \\record R (y : Nat) \\extends M
      \\record C
        | m : M
        | m => \\new M 0
      \\record D \\extends C {
        \\override m : R
      }
      """, 1);
  }

  @Test
  public void overrideImplementedError2() {
    typeCheckModule("""
      \\record M (x : Nat)
      \\record R (y : Nat) \\extends M
      \\record C
        | m : M
      \\record D \\extends C {
        | m => \\new M 0
        \\override m : R
      }
      """, 1);
  }

  @Test
  public void overrideImplementedError3() {
    typeCheckModule("""
      \\record M (x : Nat)
      \\record R (y : Nat) \\extends M
      \\record C
        | m : M
      \\record D \\extends C {
        \\override m : R
        | m => \\new M 0
      }
      """, 1);
  }

  @Test
  public void overrideImplementedError4() {
    typeCheckModule("""
      \\record M (x : Nat)
      \\record R (y : Nat) \\extends M
      \\record C
        | m : M
      \\record D \\extends C {
        \\override m : R
      }
      \\record E \\extends D
        | m => \\new M 0
      """, 1);
  }

  @Test
  public void overrideImplementedError5() {
    typeCheckModule("""
      \\record M (x : Nat)
      \\record R (y : Nat) \\extends M
      \\record C
        | m : M
      \\record D \\extends C
        | m => \\new M 0
      \\record E \\extends C {
        \\override m : R
      }
      \\record F \\extends D, E
      """, 1);
  }

  @Test
  public void overrideImplementedError6() {
    typeCheckModule("""
      \\record M (x : Nat)
      \\record R (y : Nat) \\extends M
      \\record C
        | m : M
      \\record D \\extends C
        | m => \\new M 0
      \\record E \\extends C {
        \\override m : R
      }
      \\record F \\extends E, D
      """, 1);
  }

  @Test
  public void overrideImplemented() {
    typeCheckModule("""
      \\record M (x : Nat)
      \\record R (y : Nat) \\extends M
      \\record C
        | m : R
        | m => \\new R 0 1
      \\record D \\extends C {
        \\override m : R
      }
      """);
  }

  @Test
  public void levelError() {
    typeCheckModule("""
      \\class C (X : \\hType (\\suc \\lp))
      \\class S \\extends C {
        \\override X : \\hType \\lp
      }
      \\func f (X : \\hType (\\suc \\lp)) : C X \\cowith
      \\func g (X : \\hType (\\suc \\lp)) : S \\lp \\cowith
        | C => f \\lp X
      """, 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void levelTest() {
    typeCheckModule("""
      \\class C (X : \\hType (\\suc \\lp))
      \\class S \\extends C {
        \\override X : \\hType \\lp
      }
      \\func f (X : \\hType (\\suc \\lp)) : C X \\cowith
      \\func g (X : \\hType) : S \\lp \\cowith
        | C => f \\lp X
      """);
  }

  @Test
  public void checkTest() {
    typeCheckModule("""
      \\record R (x : Nat)
      \\record S (y : Nat) \\extends R
      \\record M (f : R)
      \\record N \\extends M {
        \\override f : S
        | g : S
        | p : f = g -> Nat
      }
      \\func test (r : R) (s : S) : M r => \\new N {
        | g => s
        | p _ => 0
      }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("M.f"))));
  }

  @Test
  public void implementedTest() {
    typeCheckModule("""
      \\record R (x : Nat)
      \\record S (y : Nat) \\extends R
      \\record T (r : R)
      \\record U \\extends T {
        \\override r : S
      }
      \\func test (x : Nat) : T \\new R x
        => \\new U (\\new S 0 x)
      """);
  }
}
