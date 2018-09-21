package org.arend.classes;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.arend.typechecking.Matchers.fieldsImplementation;
import static org.arend.typechecking.Matchers.notInScope;

public class SuperClassImplementTest extends TypeCheckingTestCase  {
  @Test
  public void partialImplement() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\instance I : B\n" +
      "  | b => 0\n" +
      "  | A { | a' => 1 }\n" +
      "  | a => 2\n" +
      "  | b' => 3\n" +
      "\\func f : I.a' = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void resolvingError() {
    resolveNamesModule(
      "\\class A (a a' : Nat)\n" +
        "\\class B (b b' : Nat) \\extends A\n" +
        "\\func f => B {\n" +
        "  | A { | b => 1 }\n" +
        "  | a => 2\n" +
        "  | b' => 3\n" +
        "  }", 1);
    assertThatErrorsAre(notInScope("b"));
  }

  @Test
  public void missingField() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\instance I : B\n" +
      "  | b => 0\n" +
      "  | A { | a' => 1 }\n" +
      "  | b' => 3", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(getDefinition("A.a").getReferable())));
  }

  @Test
  public void alreadyImplementedField() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\func f => B {\n" +
      "  | b => 0\n" +
      "  | A { | a' => 1 }\n" +
      "  | a' => 2\n" +
      "  | b' => 3\n" +
      "  }", 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(getDefinition("A.a'").getReferable())));
  }

  @Test
  public void fullyImplemented() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\instance I : B\n" +
      "  | A => \\new A { | a => 1 | a' => 2 }\n" +
      "  | b => 0\n" +
      "  | b' => 3\n" +
      "\\func f : I.a = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void alreadyFullyImplementedField() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\func f => B {\n" +
      "  | b => 0\n" +
      "  | A => \\new A { | a => 1 | a' => 2 }\n" +
      "  | a' => 2\n" +
      "  | b' => 3\n" +
      "  }", 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(getDefinition("A.a'").getReferable())));
  }

  @Test
  public void fullyAndPartiallyImplemented() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\instance I : B\n" +
      "  | a => 1\n" +
      "  | A => \\new A { | a => 1 | a' => 2 }\n" +
      "  | b => 0\n" +
      "  | b' => 3\n" +
      "\\func f : I.a = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void fullyAndPartiallyIncorrectlyImplemented() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\instance I : B\n" +
      "  | a => 4\n" +
      "  | A => \\new A { | a => 1 | a' => 2 }\n" +
      "  | b => 0\n" +
      "  | b' => 3", 1);
  }

  @Test
  public void fullyAndAppImplemented() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\instance I : B 1\n" +
      "  | A => \\new A { | a => 1 | a' => 2 }\n" +
      "  | b => 0\n" +
      "  | b' => 3\n" +
      "\\func f : I.a = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void fullyAndAppIncorrectlyImplemented() {
    typeCheckModule(
      "\\class A (a a' : Nat)\n" +
      "\\class B (b b' : Nat) \\extends A\n" +
      "\\instance I : B 4\n" +
      "  | A => \\new A { | a => 1 | a' => 2 }\n" +
      "  | b => 0\n" +
      "  | b' => 3", 1);
  }
}
