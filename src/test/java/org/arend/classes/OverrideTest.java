package org.arend.classes;

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
}
