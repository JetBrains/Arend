package org.arend.typechecking.definition;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.typeMismatchError;

public class ClassFieldPropertyTest extends TypeCheckingTestCase {
  @Test
  public void nonPropPropertyError() {
    typeCheckModule(
      "\\class C {\n" +
      "  \\property p : Nat\n" +
      "}", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void propertyTest() {
    typeCheckModule(
      "\\class C {\n" +
      "  \\property p : 0 = 0\n" +
      "}");
  }

  @Test
  public void propertyNewEvalTest() {
    typeCheckModule(
      "\\class C {\n" +
      "  | p : 0 = 0\n" +
      "}\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func foo (x : 0 = 0) : p {\\new C x} = x => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void propertyFunctionEvalTest() {
    typeCheckModule(
      "\\class C {\n" +
      "  | p : 0 = 0\n" +
      "}\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func inst : C \\cowith | p => idp\n" +
      "\\func foo : p {inst} = idp => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void classesTest() {
    typeCheckModule(
      "\\class A {\n" +
      "  | p : 0 = 0 -> 0 = 0 -> 0 = 0\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | p x y => x\n" +
      "}\n" +
      "\\class C \\extends A {\n" +
      "  | p x y => y\n" +
      "}\n" +
      "\\class D \\extends B,C");
  }
}
