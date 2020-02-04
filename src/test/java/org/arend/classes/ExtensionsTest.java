package org.arend.classes;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.arend.core.expr.ExpressionFactory.Suc;
import static org.arend.core.expr.ExpressionFactory.Zero;
import static org.arend.typechecking.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExtensionsTest extends TypeCheckingTestCase {
  @Test
  public void fields() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "  | p : a = a'\n" +
        "}\n" +
        "\\func f (b : B) : \\Sigma (x : b.A) (x = b.a') => (b.a, b.p)");
  }

  @Test
  public void newTest() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "  | p : a = a'\n" +
        "}\n" +
        "\\func f => \\new B { | A => Nat | a => 0 | a' => 0 | p => idp }");
  }

  @Test
  public void newError() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "}\n" +
        "\\func f => \\new B { | A => Nat | a' => 0 }", 1);
  }

  @Test
  public void fieldEval() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "}\n" +
        "\\func f : \\Sigma (1 = 1) (0 = 0) =>\n" +
        "  \\let b : B => \\new B { | A => Nat | a => 1 | a' => 0 }\n" +
        "  \\in  (path (\\lam _ => b.a), path (\\lam _ => b.a'))");
  }

  @Test
  public void coercion() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "}\n" +
        "\\func f (a : C) => a.a\n" +
        "\\func g : 3 = 3 => path (\\lam _ => f (\\new B { | A => Nat | a' => 2 | a => 3 }))\n" +
        "\\func h (b : B { | A => Nat | a => 5 }) : 5 = 5 => path (\\lam _ => b.a)");
  }

  @Test
  public void multiple() {
    typeCheckModule(
        "\\class A {\n" +
        "  | S : \\Set0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  | b : S\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  | c : S\n" +
        "}\n" +
        "\\class D \\extends B, C {\n" +
        "  | p : b = c\n" +
        "}\n" +
        "\\func f (d : D { | S => Nat | c => 4 | b => 6 }) : 6 = 4 => d.p\n" +
        "\\func g => \\new D { | S => Nat | b => 3 | c => 3 | p => idp }");
  }

  @Test
  public void multipleInheritanceSingleImplementation() {
    typeCheckModule(
        "\\class A {\n" +
        "  | a : Nat\n" +
        "}\n" +
        "\\class Z \\extends A { | a => 0 }\n" +
        "\\class B \\extends Z\n" +
        "\\class C \\extends Z\n" +
        "\\class D \\extends B, C\n");
  }

  @Test
  public void multipleInheritanceEqualImplementations() {
    typeCheckModule(
        "\\class A {\n" +
        "  | a : Nat\n" +
        "}\n" +
        "\\class B \\extends A { | a => 0 }\n" +
        "\\class C \\extends A { | a => 0 }\n" +
        "\\class D \\extends B, C\n");
  }

  @Test
  public void internalInheritance() {
    typeCheckModule("\\class A { \\class B \\extends A }");
  }

  @Test
  public void universe() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C");
    assertEquals(new Sort(1, 1), ((ClassDefinition) getDefinition("C")).getSort());
    assertEquals(new Sort(1, 1), ((ClassDefinition) getDefinition("B")).getSort());
  }

  @Test
  public void recursiveExtendsError() {
    typeCheckModule("\\class A \\extends A", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void mutualRecursiveExtendsError() {
    typeCheckModule(
      "\\class A \\extends B\n" +
      "\\class B \\extends A", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void newEmbeddedExtension() {
    typeCheckModule(
      "\\class A { | n : Nat -> Nat | k : Nat }\n" +
      "\\class B { | m : Nat | a : A }\n" +
      "\\func f => \\new B { | m => 0 | a => \\new A { | n => \\lam x => x | k => 1 } }");
    FunCallExpression funCall = new FunCallExpression((FunctionDefinition) getDefinition("f"), Sort.STD, Collections.emptyList());

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), Sort.STD, funCall);
    Expression fieldCallANorm = fieldCallA.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallANorm instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), Sort.STD, funCall);
    fieldCallM = fieldCallM.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallM instanceof SmallIntegerExpression);
    assertEquals(0, ((SmallIntegerExpression) fieldCallM).getInteger());

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), Sort.STD, fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallK);
  }

  @Test
  public void deeplyEmbeddedExtension() {
    typeCheckModule(
      "\\class A { | n : Nat -> Nat | k : Nat }\n" +
      "\\class B { | m : Nat | a : A }\n" +
      "\\class C { | l : Nat | b : B }\n" +
      "\\func f => \\new C { | l => 2 | b { | m => 1 | a { | n => \\lam x => x | k => 0 } } }");
    FunCallExpression funCall = new FunCallExpression((FunctionDefinition) getDefinition("f"), Sort.STD, Collections.emptyList());

    Expression fieldCallL = FieldCallExpression.make((ClassField) getDefinition("C.l"), Sort.STD, funCall);
    fieldCallL = fieldCallL.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallL instanceof SmallIntegerExpression);
    assertEquals(2, ((SmallIntegerExpression) fieldCallL).getInteger());

    Expression fieldCallB = FieldCallExpression.make((ClassField) getDefinition("C.b"), Sort.STD, funCall);
    Expression fieldCallBNorm = fieldCallB.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallBNorm instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), Sort.STD, fieldCallB);
    fieldCallM = fieldCallM.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallM);

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), Sort.STD, fieldCallB);
    Expression fieldCallANorm = fieldCallA.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallANorm instanceof NewExpression);

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), Sort.STD, fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Zero(), fieldCallK);
  }

  @Test
  public void embeddedExtension() {
    typeCheckModule(
      "\\class A { | n : Nat -> Nat | k : Nat }\n" +
      "\\class B { | m : Nat | a : A }\n" +
      "\\class C \\extends B { | m => 0 | a { | n => \\lam x => x | k => 1 } }");
    NewExpression newExpr = new NewExpression(null, new ClassCallExpression((ClassDefinition) getDefinition("C"), Sort.STD));

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), Sort.STD, newExpr);
    assertTrue(fieldCallA instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), Sort.STD, newExpr);
    assertTrue(fieldCallM instanceof SmallIntegerExpression);
    assertEquals(0, ((SmallIntegerExpression) fieldCallM).getInteger());

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), Sort.STD, fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallK);
  }

  @Test
  public void anonymousEmbeddedExtension() {
    typeCheckModule(
      "\\class A { | n : Nat -> Nat | k : Nat }\n" +
      "\\class B { | m : Nat | a : A }\n" +
      "\\func f => \\new B { | m => 0 | a { | n => \\lam x => x | k => 1 } }");
    FunCallExpression funCall = new FunCallExpression((FunctionDefinition) getDefinition("f"), Sort.STD, Collections.emptyList());

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), Sort.STD, funCall);
    Expression fieldCallANorm = fieldCallA.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallANorm instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), Sort.STD, funCall);
    fieldCallM = fieldCallM.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallM instanceof SmallIntegerExpression);
    assertEquals(0, ((SmallIntegerExpression) fieldCallM).getInteger());

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), Sort.STD, fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallK);
  }

  @Test
  public void instanceEmbeddedExtension() {
    typeCheckModule(
      "\\class A { | n : Nat -> Nat | k : Nat }\n" +
      "\\class B { | m : Nat | a : A }\n" +
      "\\instance f : B | m => 0 | a { | n => \\lam x => x | k => 1 }");
    FunCallExpression funCall = new FunCallExpression((FunctionDefinition) getDefinition("f"), Sort.STD, Collections.emptyList());

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), Sort.STD, funCall);
    Expression fieldCallANorm = fieldCallA.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallANorm instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), Sort.STD, funCall);
    fieldCallM = fieldCallM.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallM instanceof SmallIntegerExpression);
    assertEquals(0, ((SmallIntegerExpression) fieldCallM).getInteger());

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), Sort.STD, fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallK);
  }

  @Test
  public void expectedTypeExtension() {
    typeCheckModule(
      "\\record A (x : Nat) (y : x = x)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A) : A => \\new B { | A => a }", 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension2() {
    typeCheckModule(
      "\\record A (x : Nat) (y : x = x)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A 0) : A => \\new B { | A => a }");
  }

  @Test
  public void expectedTypeExtension3() {
    typeCheckModule(
      "\\record A (x : Nat) (y : x = x)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A 1) : A => \\new B { | A => a }", 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension4() {
    typeCheckModule(
      "\\record A (x : Nat) (y : x = x)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A) : A 0 => \\new B { | A => a }", 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension5() {
    typeCheckModule(
      "\\record A (x : Nat) (y : x = x)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A 0) : A 0 => \\new B { | A => a }");
  }

  @Test
  public void expectedTypeExtension6() {
    typeCheckModule(
      "\\record A (x : Nat) (y : x = x)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A 1) : A 0 => \\new B { | A => a }", 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension7() {
    typeCheckModule(
      "\\record A (x : Nat) (y : x = x)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A 1) : A 1 => \\new B { | A => a }", 2);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))), typeMismatchError());
  }

  @Test
  public void expectedTypeExtension8() {
    typeCheckModule(
      "\\record A (x y : Nat)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A 1) : A 0 => \\new B { | A => a }", 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension9() {
    typeCheckModule(
      "\\record A (x y : Nat)\n" +
      "\\record B \\extends A | x => 0\n" +
      "\\func f (a : A 1) : A 1 => \\new B { | A => a }", 2);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))), typeMismatchError());
  }

  @Test
  public void universesTest() {
    typeCheckModule(
      "\\record C (A : \\Type) (a : A)\n" +
      "\\func f (c : C \\level 1 1 Nat) : C \\level 0 1 => c");
  }

  @Test
  public void universesTestError() {
    typeCheckModule(
      "\\record C (A : \\Type) (a : A)\n" +
      "\\func f (c : C \\level 1 1 \\Set0) : C \\level 0 1 => c", 1);
  }

  @Test
  public void comparisonTest() {
    typeCheckModule(
      "\\record C (A : \\Type) (a : A)\n" +
      "\\func f (c : C \\Type4 \\Type3) : C \\Type5 \\Type3 => c");
  }

  @Test
  public void comparisonTest2() {
    typeCheckModule(
      "\\record C (A : \\Type) (a : A)\n" +
      "\\func f (c : C \\Type5 \\Type3) : C \\Type5 \\Type4 => c", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void comparisonTest3() {
    typeCheckModule(
      "\\record C (A : \\Type -> \\Type)\n" +
      "\\func f (c : C (\\lam _ => \\Type4)) : C (\\lam _ => \\Type5) => c", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void extensionParameterError() {
    typeCheckModule(
      "\\class C\n" +
      "\\func f {c : C} => 0\n" +
      "\\class D (X : \\Type) \\extends C\n" +
      "  | g : f = f", 2);
  }

  @Test
  public void extensionParameterTest() {
    typeCheckModule(
      "\\class C\n" +
      "\\func f {c : C} => 0\n" +
      "\\class D \\noclassifying (X : \\Type) \\extends C\n" +
      "  | g : f = f\n" +
      "");
  }

  @Test
  public void extensionParameterError2() {
    typeCheckModule(
      "\\class C (X : \\Type)\n" +
      "\\class D \\extends C\n" +
      "  | x : X\n" +
      "\\func test (d : D) => x", 1);
  }

  @Test
  public void extensionParameterTest2() {
    typeCheckModule(
      "\\class C (X : \\Type)\n" +
      "  | x : X\n" +
      "\\class D \\noclassifying \\extends C\n" +
      "  | y : X\n" +
      "\\func test (d : D) => d.x = {d.X} y");
  }
}
