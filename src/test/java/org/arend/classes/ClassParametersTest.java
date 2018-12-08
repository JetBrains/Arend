package org.arend.classes;

import org.arend.core.definition.ClassDefinition;
import org.arend.typechecking.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.notInScope;
import static org.arend.typechecking.Matchers.typeMismatchError;
import static junit.framework.TestCase.assertEquals;

public class ClassParametersTest extends TypeCheckingTestCase {
  @Test
  public void classParameters() {
    ClassDefinition def = (ClassDefinition) typeCheckDef("\\class C {A1 : \\Set} (a2 a3 : Nat) {A4 : \\Set} { | a1 : A1 | p : a2 = a3 | a4 : A4 }");
    assertEquals(7, def.getPersonalFields().size());
  }

  @Test
  public void parameterField() {
    resolveNamesModule(
      "\\class A (n : Nat)\n" +
      "\\func f (a : A) : Nat => n a", 1);
    assertThatErrorsAre(notInScope("n"));
  }

  @Test
  public void parameterFieldAccessor() {
    typeCheckModule(
      "\\class A (n : Nat)\n" +
      "\\func f (a : A) : Nat => a.n");
  }

  @Test
  public void doCoerce() {
    typeCheckModule(
      "\\class C (A : \\Set) {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func f (c : C) : \\Set => c\n" +
      "\\func g (n : f (\\new C { | A => Nat | a => 0 })) => suc n");
  }

  @Test
  public void coerceType() {
    typeCheckModule(
      "\\class C (A : \\Set) {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func f (c : C { | A => Nat }) : c => 1\n" +
      "\\func g : f (\\new C { | A => Nat | a => 0 }) = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void doNotCoerce() {
    typeCheckModule(
      "\\class C (A : \\Set) {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func f (c : C) : Nat => c", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void coerceExtendsExplicit() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
      "\\class D (B : \\Set) \\extends C");
  }

  @Test
  public void coerceExtendsImplicit() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
      "\\class D \\extends C { | a : A }\n" +
      "\\func f (d : D) : \\Set => d\n" +
      "\\func g (n : f (\\new D { | A => Nat | a => 0 })) => suc n");
  }

  @Test
  public void coerceExtendsMultiple() {
    typeCheckModule(
      "\\class C1 (A : Nat)\n" +
      "\\class C2 (B : \\Set)\n" +
      "\\class D \\extends C1, C2");
  }

  @Test
  public void doNotCoerceExtendsMultipleWithParameter() {
    typeCheckModule(
      "\\class C1 (n : Nat)\n" +
      "\\class C2 (B : \\Set0)\n" +
      "\\class D \\extends C2, C1\n" +
      "\\func d => \\new D { | n => 0 | B => \\Prop }\n" +
      "\\func test3 : 0 = d => path (\\lam _ => 0)\n", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void coerceExtendsMultipleWithParameter() {
    typeCheckModule(
      "\\class C1 (n : Nat)\n" +
      "\\class C2 (B : \\1-Type1)\n" +
      "\\class D (m : Nat) \\extends C1, C2 { | B => \\Set0 | b : B }\n" +
      "\\func f (d : D) : d.B => d.b\n" +
      "\\func g (d : D) : Nat => d.n\n" +
      "\\func h (d : D) : Nat => d.m\n" +
      "\\func d => \\new D { | m => 0 | n => 1 | b => \\Prop }\n" +
      "\\func test1 : g d = 1 => path (\\lam _ => 1)\n" +
      "\\func test2 : h d = 0 => path (\\lam _ => 0)\n" +
      "\\func test3 : 1 = d => path (\\lam _ => 1)\n");
  }


  @Test
  public void parameterError() {
    typeCheckModule("\\class C (x y : zero = suc)", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void classCall() {
    typeCheckModule(
      "\\class C (x : Nat)\n" +
      "\\func f => C 0");
  }

  @Test
  public void classCallImplicit() {
    typeCheckModule(
      "\\class C {x : Nat} (y : Nat) {z w : Nat} (p : z = w)\n" +
      "\\func f => C {0} 1 (path (\\lam _ => 2))");
  }

  @Test
  public void classCallFields() {
    typeCheckModule(
      "\\class C {x : Nat} (y : Nat) { | z : Nat -> Nat | p : Nat }\n" +
      "\\func f => C {0} 1 (\\lam x => x)");
  }

  @Test
  public void classCallFieldAccessors() {
    typeCheckModule(
      "\\class C {x : Nat} (y : Nat) { | z : Nat -> Nat | p : Nat }\n" +
      "\\func f (c : C {0} 1 (\\lam x => x)) : Nat => c.p");
  }

  @Test
  public void classCallImplicitError() {
    typeCheckModule(
      "\\class C (x : Nat)\n" +
      "\\func f => C {0} 1", 1);
  }

  @Test
  public void tooManyArguments() {
    typeCheckModule(
      "\\class C (x : Nat)\n" +
      "\\func f => C 1 2", 1);
  }

  @Test
  public void superParameters() {
    typeCheckModule(
      "\\class C { | x : Nat }\n" +
      "\\class D (y : Nat -> Nat) \\extends C\n" +
      "\\func f => D 1 (\\lam x => x)");
  }

  @Test
  public void superParameters2() {
    typeCheckModule(
      "\\class C (x y : Nat)\n" +
      "\\class D (z w : Nat) \\extends C\n" +
      "\\func f => \\new D { | x => 0 | y => 1 | z => 2 | w => 3 }");
  }

  @Test
  public void superImplicitParameters() {
    typeCheckModule(
      "\\class C (x : Nat) {y : Nat} (p : y = 0)\n" +
      "\\class D {z : Nat} (w : z = 1) \\extends C\n" +
      "\\func f => \\new D 0 (path (\\lam _ => 0)) (path (\\lam _ => 1))");
  }

  @Test
  public void superImplicitParameters2() {
    typeCheckModule(
      "\\class C (x : Nat) {y : Nat}\n" +
      "\\class D {z : Nat} \\extends C\n" +
      "\\func f => D 0\n" +
      "\\func g => \\new D 0 { | y => 1 | z => 2 }");
  }

  @Test
  public void classImplicitParameter() {
    typeCheckModule(
      "\\class C (X : \\Type) | x : X\n" +
      "\\class D {A : C} (i : A)\n" +
      "\\func f {A : C} (i : A) => \\new D i");
  }
}
