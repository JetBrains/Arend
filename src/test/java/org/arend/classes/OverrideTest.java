package org.arend.classes;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.Matchers.typeMismatchError;

public class OverrideTest extends TypeCheckingTestCase {
  @Test
  public void subtypeTest() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A | a => 0\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C { \\override f : B }\n" +
      "\\func test (d : D) : d.f.a = 0 => idp");
  }

  @Test
  public void subtypeError() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat)\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C { \\override f : B }", 1);
  }

  @Test
  public void resolveError() {
    resolveNamesModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class C | g : A\n" +
      "\\class D \\extends C { \\override f : B }", 1);
  }

  @Test
  public void overrideOwnFieldError() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class C | g : A\n" +
      "\\class D \\extends C { | f : A \\override f : B }", 1);
  }

  @Test
  public void propertyTest() {
    typeCheckModule(
      "\\class A\n" +
      "\\class B \\extends A\n" +
      "\\class C { \\property f : A }\n" +
      "\\class D \\extends C { \\override f : B }");
  }

  @Test
  public void propertyFieldError() {
    typeCheckModule(
      "\\class A\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class C { \\property f : A }\n" +
      "\\class D \\extends C { \\override f : B }", 1);
  }

  @Test
  public void extensionTest() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class B' (b' : Nat) \\extends A\n" +
      "\\class L \\extends B, B' | a => 0\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C { \\override f : B }\n" +
      "\\class D' \\extends C { \\override f : B' }\n" +
      "\\class K \\extends D, D' { \\override f : L }\n" +
      "\\func test (k : K) : k.f.a = 0 => idp");
  }

  @Test
  public void extensionError() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class L \\extends A | a => 0\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C { \\override f : B }\n" +
      "\\class E \\extends D\n" +
      "\\class F \\extends E { \\override f : L }", 1);
  }

  @Test
  public void extensionConflictTest() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C { \\override f : B }\n" +
      "\\class D' \\extends C { \\override f : B }\n" +
      "\\class E \\extends D, D'");
  }

  @Test
  public void extensionConflictError() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class B' (b' : Nat) \\extends A\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C { \\override f : B }\n" +
      "\\class D' \\extends C { \\override f : B' }\n" +
      "\\class E \\extends D, D'", 1);
  }

  @Test
  public void overriddenFieldTypeTest() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C {\n" +
      "  \\override f : B\n" +
      "}\n" +
      "\\func test (d : D) : B => d.f");
  }

  @Test
  public void dependentFieldsTest() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C {\n" +
      "  \\override f : B\n" +
      "  | g : f.a = B.b {f}\n" +
      "}");
  }

  @Test
  public void dependentFieldsError() {
    typeCheckModule(
      "\\class A (a : Nat)\n" +
      "\\class B (b : Nat) \\extends A\n" +
      "\\class C | f : A\n" +
      "\\class D \\extends C {\n" +
      "  | g : f.a = B.b {f}\n" +
      "  \\override f : B\n" +
      "}", 1);
  }

  @Test
  public void newTest() {
    typeCheckModule(
      "\\record R\n" +
      "\\record S \\extends R\n" +
      "\\record A | foo : R\n" +
      "\\record B \\extends A {\n" +
      "  \\override foo : S\n" +
      "}\n" +
      "\\func test => B (\\new R)", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void implementTest() {
    typeCheckModule(
      "\\record R\n" +
      "\\record S \\extends R\n" +
      "\\record S' \\extends R\n" +
      "\\record A | foo : R\n" +
      "\\record B \\extends A {\n" +
      "  \\override foo : S\n" +
      "}\n" +
      "\\record C \\extends B\n" +
      "  | foo => \\new R", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void derivedOverridePi() {
    typeCheckModule(
      "\\record R (X : \\Set (\\suc \\lp)) (f : X -> X)\n" +
      "\\record S \\extends R {\n" +
      "  \\override X : \\Set \\lp\n" +
      "}\n" +
      "\\func test (A : \\Set \\lp) : \\Set \\lp => S \\lp A");
  }

  @Test
  public void derivedOverrideSigma() {
    typeCheckModule(
      "\\record R (X : \\Set (\\suc \\lp)) (f : \\Sigma X X)\n" +
      "\\record S \\extends R {\n" +
      "  \\override X : \\Set \\lp\n" +
      "}\n" +
      "\\func test (A : \\Set \\lp) : \\Set \\lp => S \\lp A");
  }

  @Test
  public void overrideImplementedError() {
    typeCheckModule(
      "\\record M (x : Nat)\n" +
      "\\record R (y : Nat) \\extends M\n" +
      "\\record C\n" +
      "  | m : M\n" +
      "  | m => \\new M 0\n" +
      "\\record D \\extends C {\n" +
      "  \\override m : R\n" +
      "}", 1);
  }

  @Test
  public void overrideImplementedError2() {
    typeCheckModule(
      "\\record M (x : Nat)\n" +
      "\\record R (y : Nat) \\extends M\n" +
      "\\record C\n" +
      "  | m : M\n" +
      "\\record D \\extends C {\n" +
      "  | m => \\new M 0\n" +
      "  \\override m : R\n" +
      "}", 1);
  }

  @Test
  public void overrideImplementedError3() {
    typeCheckModule(
      "\\record M (x : Nat)\n" +
      "\\record R (y : Nat) \\extends M\n" +
      "\\record C\n" +
      "  | m : M\n" +
      "\\record D \\extends C {\n" +
      "  \\override m : R\n" +
      "  | m => \\new M 0\n" +
      "}", 1);
  }

  @Test
  public void overrideImplementedError4() {
    typeCheckModule(
      "\\record M (x : Nat)\n" +
      "\\record R (y : Nat) \\extends M\n" +
      "\\record C\n" +
      "  | m : M\n" +
      "\\record D \\extends C {\n" +
      "  \\override m : R\n" +
      "}\n" +
      "\\record E \\extends D\n" +
      "  | m => \\new M 0", 1);
  }

  @Test
  public void overrideImplementedError5() {
    typeCheckModule(
      "\\record M (x : Nat)\n" +
      "\\record R (y : Nat) \\extends M\n" +
      "\\record C\n" +
      "  | m : M\n" +
      "\\record D \\extends C\n" +
      "  | m => \\new M 0\n" +
      "\\record E \\extends C {\n" +
      "  \\override m : R\n" +
      "}\n" +
      "\\record F \\extends D, E", 1);
  }

  @Test
  public void overrideImplementedError6() {
    typeCheckModule(
      "\\record M (x : Nat)\n" +
      "\\record R (y : Nat) \\extends M\n" +
      "\\record C\n" +
      "  | m : M\n" +
      "\\record D \\extends C\n" +
      "  | m => \\new M 0\n" +
      "\\record E \\extends C {\n" +
      "  \\override m : R\n" +
      "}\n" +
      "\\record F \\extends E, D", 1);
  }

  @Test
  public void overrideImplemented() {
    typeCheckModule(
      "\\record M (x : Nat)\n" +
      "\\record R (y : Nat) \\extends M\n" +
      "\\record C\n" +
      "  | m : R\n" +
      "  | m => \\new R 0 1\n" +
      "\\record D \\extends C {\n" +
      "  \\override m : R\n" +
      "}");
  }

  @Test
  public void levelError() {
    typeCheckModule(
      "\\class C (X : \\hType (\\suc \\lp))\n" +
      "\\class S \\extends C {\n" +
      "  \\override X : \\hType \\lp\n" +
      "}\n" +
      "\\func f (X : \\hType (\\suc \\lp)) : C X \\cowith\n" +
      "\\func g (X : \\hType (\\suc \\lp)) : S \\lp \\cowith\n" +
      "  | C => f \\lp X", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void levelTest() {
    typeCheckModule(
      "\\class C (X : \\hType (\\suc \\lp))\n" +
      "\\class S \\extends C {\n" +
      "  \\override X : \\hType \\lp\n" +
      "}\n" +
      "\\func f (X : \\hType (\\suc \\lp)) : C X \\cowith\n" +
      "\\func g (X : \\hType) : S \\lp \\cowith\n" +
      "  | C => f \\lp X");
  }
}
