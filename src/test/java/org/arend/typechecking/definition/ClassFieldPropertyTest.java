package org.arend.typechecking.definition;

import org.arend.core.definition.ClassField;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.typeMismatchError;
import static org.junit.Assert.assertTrue;

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
      "\\func foo (x : 0 = 0) : p {\\new C x} = x => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void propertyFunctionEvalTest() {
    typeCheckModule(
      "\\class C {\n" +
      "  | p : 0 = 0\n" +
      "}\n" +
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

  @Test
  public void propertySetLevel() {
    typeCheckModule(
      "\\class A {\n" +
      "  \\property f : \\level Nat (\\lam (x y : Nat) (p q : x = y) => Path.inProp p q)\n" +
      "}", 1);
  }

  @Test
  public void propertyLevel() {
    typeCheckModule(
      "\\class A {\n" +
      "  \\property f (A : \\Type) (p : \\Pi (x y : A) -> x = y) : \\level A p\n" +
      "}");
  }

  @Test
  public void fieldLevel() {
    resolveNamesDef(
      "\\class A {\n" +
      "  \\field f (A : \\Type) (p : \\Pi (x y : A) -> x = y) : \\level A p\n" +
      "}", 1);
  }

  @Test
  public void propertyLevel2() {
    typeCheckModule(
      "\\class A {\n" +
      "  | f (A : \\Type) : \\level ((\\Pi (x y : A) -> x = y) -> A) (\\lam (f g : (\\Pi (x y : A) -> x = y) -> A) => path (\\lam i (p : \\Pi (x y : A) -> x = y) => p (f p) (g p) @ i))\n" +
      "}");
    assertTrue(((ClassField) getDefinition("A.f")).isProperty());
  }
}
