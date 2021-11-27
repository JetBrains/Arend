package org.arend.classes;

import org.arend.Matchers;
import org.arend.core.definition.ClassDefinition;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.NotEqualExpressionsError;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.arend.Matchers.notInScope;
import static org.arend.Matchers.typeMismatchError;

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
      "\\func g : f (\\new C { | A => Nat | a => 0 }) = 1 => idp");
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
  public void coerceExtendsMultipleWithParameter() {
    typeCheckModule(
      "\\class C1 (n : Nat)\n" +
      "\\class C2 (B : \\1-Type1)\n" +
      "\\class D (m : Nat) \\extends C1, C2 { | B => \\Set0 | b : B }\n" +
      "\\func f (d : D) : d.B => d.b\n" +
      "\\func g (d : D) : Nat => d.n\n" +
      "\\func h (d : D) : Nat => d.m\n" +
      "\\func d => \\new D { | m => 0 | n => 1 | b => \\Prop }\n" +
      "\\func test1 : g d = 1 => idp\n" +
      "\\func test2 : h d = 0 => idp\n" +
      "\\func test3 : 1 = d => idp\n");
  }

  @Test
  public void coerceExtendsMultipleWithParameter2() {
    typeCheckModule(
      "\\class C1 (n : Nat)\n" +
      "\\class C2 (B : \\Set0)\n" +
      "\\class D \\extends C2, C1\n" +
      "\\func d => \\new D { | n => 0 | B => \\Prop }\n" +
      "\\func test3 : 0 = d => idp");
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
      "\\func f => \\new D 0 idp idp");
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

  @Test
  public void testImplementedField() {
    typeCheckModule(
      "\\record R {x : Nat} (p : x = 0) | q : p = p\n" +
      "\\record T {z : Nat} \\extends R { | x => z }\n" +
      "\\func f => \\new T { | p => idp | q => idp | z => 0 }", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void testImplementedField2() {
    typeCheckModule(
      "\\record R {x : Nat} (p : x = 0) | q : p = p\n" +
      "\\record T {z : Nat} \\extends R { | x => z }\n" +
      "\\func f => \\new T { | z => 0 | p => idp | q => idp }");
  }

  @Test
  public void explicitClassifyingField() {
    typeCheckModule(
      "\\class C (n : Nat) (\\classifying A : \\Type) | a : A -> 0 = n\n" +
      "\\func f (X : C 1) (x : X) : 0 = 1 => a x");
  }

  @Test
  public void recordExplicitClassifyingField() {
    resolveNamesDef("\\record C (n : Nat) (\\classifying A : \\Type) | a : A -> 0 = n", 1);
  }

  @Test
  public void tailImplicitParameters() {
    typeCheckModule(
      "\\class C | nn : Nat\n" +
      "\\instance cc : C | nn => 3\n" +
      "\\record R {x : C}\n" +
      "\\func f (r : R) : r.x.nn = 3 => idp");
  }

  @Test
  public void tailImplicitParametersArgs() {
    typeCheckModule(
      "\\class C | nn : Nat\n" +
      "\\instance cc : C | nn => 3\n" +
      "\\record R (n : Nat) {x : C}\n" +
      "\\func f (r : R 0) : r.x.nn = 3 => idp");
  }

  @Test
  public void tailImplicitParametersClassified() {
    typeCheckModule(
      "\\class C (X : \\Type) | xx : X\n" +
      "\\instance cc : C Nat | xx => 3\n" +
      "\\record R {x : C Nat}\n" +
      "\\func f (r : R) : r.x.xx = 3 => idp");
  }

  @Test
  public void tailImplicitParametersClassifiedArgs() {
    typeCheckModule(
      "\\class C (X : \\Type) | xx : X\n" +
      "\\instance cc : C Nat | xx => 3\n" +
      "\\record R (n : Nat) {x : C Nat}\n" +
      "\\func f (r : R 0) : r.x.xx = 3 => idp");
  }

  @Test
  public void tailImplicitParametersExt() {
    typeCheckModule(
      "\\class C | nn : Nat\n" +
      "\\record R {x : C}\n" +
      "\\func f => R {}");
  }

  @Test
  public void tailImplicitParametersArgsExt() {
    typeCheckModule(
      "\\class C | nn : Nat\n" +
      "\\record R (n : Nat) {x : C}\n" +
      "\\func f => R 0 {}");
  }

  @Test
  public void tailImplicitParametersExtError() {
    typeCheckModule(
      "\\class C | nn : Nat\n" +
      "\\record R {x : C}\n" +
      "\\func f => R", 1);
  }

  @Test
  public void tailImplicitParametersArgsExtError() {
    typeCheckModule(
      "\\class C | nn : Nat\n" +
      "\\record R (n : Nat) {x : C}\n" +
      "\\func f => R 0", 1);
  }

  @Test
  public void tailRecordImplicitParameters() {
    typeCheckModule(
      "\\record R (x : Nat)\n" +
      "\\class C (x : Nat) {y : R}\n" +
      "\\func f => C 0");
  }

  @Test
  public void notTailImplicitParametersExtError() {
    typeCheckModule(
      "\\class C | nn : Nat\n" +
      "\\record R {x : C} (y : Nat)\n" +
      "\\func f => R {\\new C 0} { | y => 0}");
  }

  @Test
  public void lamTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f : Nat -> Nat -> \\Type => R\n" +
      "\\func g : f 0 1 => \\new R");
  }

  @Test
  public void lamTest2() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f : Nat -> \\Type => R 0\n" +
      "\\func g : f 1 => \\new R");
  }

  @Test
  public void implicitParameter() {
    typeCheckModule(
      "\\class C (nn : Nat)\n" +
      "\\record R {x : C} (y : Nat)\n" +
      "\\func test => R {\\new C 3} 1");
  }

  @Test
  public void implicitParameterNew() {
    typeCheckModule(
      "\\class C (nn : Nat)\n" +
      "\\record R {x : C} (y : Nat)\n" +
      "\\func test => \\new R {\\new C 3} 1");
  }
}
