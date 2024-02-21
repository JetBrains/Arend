package org.arend.classes;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.arend.Matchers.fieldsImplementation;
import static org.arend.Matchers.notInScope;

public class SuperClassImplementTest extends TypeCheckingTestCase  {
  @Test
  public void partialImplement() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\instance I : B
        | b => 0
        | A { | a' => 1 }
        | a => 2
        | b' => 3
      \\func f : I.a' = 1 => idp
      """);
  }

  @Test
  public void resolvingError() {
    resolveNamesModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\func f => B {
        | A { | b => 1 }
        | a => 2
        | b' => 3
        }
      """, 1);
    assertThatErrorsAre(notInScope("b"));
  }

  @Test
  public void missingField() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\instance I : B
        | b => 0
        | A { | a' => 1 }
        | b' => 3
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(getDefinition("A.a").getReferable())));
  }

  @Test
  public void alreadyImplementedField() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\func f => B {
        | b => 0
        | A { | a' => 1 }
        | a' => 2
        | b' => 3
        }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(getDefinition("A.a'").getReferable())));
  }

  @Test
  public void fullyImplemented() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\instance I : B
        | A => \\new A { | a => 1 | a' => 2 }
        | b => 0
        | b' => 3
      \\func f : I.a = 1 => idp
      """);
  }

  @Test
  public void alreadyFullyImplementedField() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\func f => B {
        | b => 0
        | A => \\new A { | a => 1 | a' => 2 }
        | a' => 2
        | b' => 3
      }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(getDefinition("A.a'").getReferable())));
  }

  @Test
  public void fullyAndPartiallyImplemented() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\instance I : B
        | A => \\new A { | a => 1 | a' => 2 }
        | b => 0
        | b' => 3
      \\func f : I.a = 1 => idp
      """);
  }

  @Test
  public void fullyAndPartiallyIncorrectlyImplemented() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\instance I : B
        | a => 1
        | A => \\new A { | a => 1 | a' => 2 }
        | b => 0
        | b' => 3
      """);
  }

  @Test
  public void fullyAndAppImplemented() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\instance I : B 0 2
        | a => 1
        | a' => 3
      \\func f : I.a = 1 => idp
      """);
  }

  @Test
  public void fullyAndAppIncorrectlyImplemented() {
    typeCheckModule("""
      \\class A (a a' : Nat)
      \\class B (b b' : Nat) \\extends A
      \\instance I : B { | a => 1 }
        | A => \\new A { | a => 1 | a' => 2 }
        | b => 0
        | b' => 3
      """);
  }
}
