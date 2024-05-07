package org.arend.classes;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.Levels;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.arend.Matchers.*;
import static org.arend.core.expr.ExpressionFactory.Suc;
import static org.arend.core.expr.ExpressionFactory.Zero;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExtensionsTest extends TypeCheckingTestCase {
  @Test
  public void fields() {
    typeCheckModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C {
        | a' : A
        | p : a = a'
      }
      \\func f (b : B) : \\Sigma (x : b.A) (x = b.a') => (b.a, b.p)
      """);
  }

  @Test
  public void newTest() {
    typeCheckModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C {
        | a' : A
        | p : a = a'
      }
      \\func f => \\new B { | A => Nat | a => 0 | a' => 0 | p => idp }
      """);
  }

  @Test
  public void newError() {
    typeCheckModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C {
        | a' : A
      }
      \\func f => \\new B { | A => Nat | a' => 0 }
      """, 1);
  }

  @Test
  public void fieldEval() {
    typeCheckModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C {
        | a' : A
      }
      \\func f : \\Sigma (1 = 1) (0 = 0) =>
        \\let b : B => \\new B { | A => Nat | a => 1 | a' => 0 }
        \\in  (path (\\lam _ => b.a), path (\\lam _ => b.a'))
      """);
  }

  @Test
  public void coercion() {
    typeCheckModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C {
        | a' : A
      }
      \\func f (a : C) => a.a
      \\func g : 3 = 3 => path (\\lam _ => f (\\new B { | A => Nat | a' => 2 | a => 3 }))
      \\func h (b : B { | A => Nat | a => 5 }) : 5 = 5 => path (\\lam _ => b.a)
      """);
  }

  @Test
  public void multiple() {
    typeCheckModule("""
      \\class A {
        | S : \\Set0
      }
      \\class B \\extends A {
        | b : S
      }
      \\class C \\extends A {
        | c : S
      }
      \\class D \\extends B, C {
        | p : b = c
      }
      \\lemma f (d : D { | S => Nat | c => 4 | b => 6 }) : 6 = 4 => d.p
      \\func g => \\new D { | S => Nat | b => 3 | c => 3 | p => idp }
      """);
  }

  @Test
  public void multipleInheritanceSingleImplementation() {
    typeCheckModule("""
      \\class A {
        | a : Nat
      }
      \\class Z \\extends A { | a => 0 }
      \\class B \\extends Z
      \\class C \\extends Z
      \\class D \\extends B, C
      """);
  }

  @Test
  public void multipleInheritanceEqualImplementations() {
    typeCheckModule("""
      \\class A {
        | a : Nat
      }
      \\class B \\extends A { | a => 0 }
      \\class C \\extends A { | a => 0 }
      \\class D \\extends B, C
      """);
  }

  @Test
  public void internalInheritance() {
    typeCheckModule("\\class A { \\class B \\extends A }");
  }

  @Test
  public void universe() {
    typeCheckModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C
      """);
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
    typeCheckModule("""
      \\class A { | n : Nat -> Nat | k : Nat }
      \\class B { | m : Nat | a : A }
      \\func f => \\new B { | m => 0 | a => \\new A { | n => \\lam x => x | k => 1 } }
      """);
    Expression funCall = FunCallExpression.make((FunctionDefinition) getDefinition("f"), Levels.EMPTY, Collections.emptyList());

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), funCall);
    Expression fieldCallANorm = fieldCallA.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallANorm instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), funCall);
    fieldCallM = fieldCallM.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallM instanceof SmallIntegerExpression);
    assertEquals(0, ((SmallIntegerExpression) fieldCallM).getInteger());

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallK);
  }

  @Test
  public void deeplyEmbeddedExtension() {
    typeCheckModule("""
      \\class A { | n : Nat -> Nat | k : Nat }
      \\class B { | m : Nat | a : A }
      \\class C { | l : Nat | b : B }
      \\func f => \\new C { | l => 2 | b { | m => 1 | a { | n => \\lam x => x | k => 0 } } }
      """);
    Expression funCall = FunCallExpression.make((FunctionDefinition) getDefinition("f"), Levels.EMPTY, Collections.emptyList());

    Expression fieldCallL = FieldCallExpression.make((ClassField) getDefinition("C.l"), funCall);
    fieldCallL = fieldCallL.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallL instanceof SmallIntegerExpression);
    assertEquals(2, ((SmallIntegerExpression) fieldCallL).getInteger());

    Expression fieldCallB = FieldCallExpression.make((ClassField) getDefinition("C.b"), funCall);
    Expression fieldCallBNorm = fieldCallB.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallBNorm instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), fieldCallB);
    fieldCallM = fieldCallM.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallM);

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), fieldCallB);
    Expression fieldCallANorm = fieldCallA.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallANorm instanceof NewExpression);

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Zero(), fieldCallK);
  }

  @Test
  public void embeddedExtension() {
    typeCheckModule("""
      \\class A { | n : Nat -> Nat | k : Nat }
      \\class B { | m : Nat | a : A }
      \\class C \\extends B { | m => 0 | a { | n => \\lam x => x | k => 1 } }
      """);
    NewExpression newExpr = new NewExpression(null, new ClassCallExpression((ClassDefinition) getDefinition("C"), Levels.EMPTY));

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), newExpr);
    assertTrue(fieldCallA instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), newExpr);
    assertTrue(fieldCallM instanceof SmallIntegerExpression);
    assertEquals(0, ((SmallIntegerExpression) fieldCallM).getInteger());

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallK);
  }

  @Test
  public void anonymousEmbeddedExtension() {
    typeCheckModule("""
      \\class A { | n : Nat -> Nat | k : Nat }
      \\class B { | m : Nat | a : A }
      \\func f => \\new B { | m => 0 | a { | n => \\lam x => x | k => 1 } }
      """);
    Expression funCall = FunCallExpression.make((FunctionDefinition) getDefinition("f"), Levels.EMPTY, Collections.emptyList());

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), funCall);
    Expression fieldCallANorm = fieldCallA.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallANorm instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), funCall);
    fieldCallM = fieldCallM.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallM instanceof SmallIntegerExpression);
    assertEquals(0, ((SmallIntegerExpression) fieldCallM).getInteger());

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallK);
  }

  @Test
  public void instanceEmbeddedExtension() {
    typeCheckModule("""
      \\class A { | n : Nat -> Nat | k : Nat }
      \\class B { | m : Nat | a : A }
      \\instance f : B | m => 0 | a { | n => \\lam x => x | k => 1 }
      """);
    Expression funCall = FunCallExpression.make((FunctionDefinition) getDefinition("f"), Levels.EMPTY, Collections.emptyList());

    Expression fieldCallA = FieldCallExpression.make((ClassField) getDefinition("B.a"), funCall);
    Expression fieldCallANorm = fieldCallA.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallANorm instanceof NewExpression);

    Expression fieldCallM = FieldCallExpression.make((ClassField) getDefinition("B.m"), funCall);
    fieldCallM = fieldCallM.normalize(NormalizationMode.WHNF);
    assertTrue(fieldCallM instanceof SmallIntegerExpression);
    assertEquals(0, ((SmallIntegerExpression) fieldCallM).getInteger());

    Expression fieldCallK = FieldCallExpression.make((ClassField) getDefinition("A.k"), fieldCallA);
    fieldCallK = fieldCallK.normalize(NormalizationMode.WHNF);
    assertEquals(Suc(Zero()), fieldCallK);
  }

  @Test
  public void expectedTypeExtension() {
    typeCheckModule("""
      \\record A (x : Nat) (y : x = x)
      \\record B \\extends A | x => 0
      \\func f (a : A) : A => \\new B { | A => a }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension2() {
    typeCheckModule("""
      \\record A (x : Nat) (y : x = x)
      \\record B \\extends A | x => 0
      \\func f (a : A 0) : A => \\new B { | A => a }
      """);
  }

  @Test
  public void expectedTypeExtension3() {
    typeCheckModule("""
      \\record A (x : Nat) (y : x = x)
      \\record B \\extends A | x => 0
      \\func f (a : A 1) : A => \\new B { | A => a }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension4() {
    typeCheckModule("""
      \\record A (x : Nat) (y : x = x)
      \\record B \\extends A | x => 0
      \\lemma f (a : A) : A 0 => \\new B { | A => a }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension5() {
    typeCheckModule("""
      \\record A (x : Nat) (y : x = x)
      \\record B \\extends A | x => 0
      \\lemma f (a : A 0) : A 0 => \\new B { | A => a }
      """);
  }

  @Test
  public void expectedTypeExtension6() {
    typeCheckModule("""
      \\record A (x : Nat) (y : x = x)
      \\record B \\extends A | x => 0
      \\lemma f (a : A 1) : A 0 => \\new B { | A => a }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension7() {
    typeCheckModule("""
      \\record A (x : Nat) (y : x = x)
      \\record B \\extends A | x => 0
      \\func f (a : A 1) : A 1 => \\new B { | A => a }
      """, 2);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))), typeMismatchError());
  }

  @Test
  public void expectedTypeExtension8() {
    typeCheckModule("""
      \\record A (x y : Nat)
      \\record B \\extends A | x => 0
      \\func f (a : A 1) : A 0 => \\new B { | A => a }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))));
  }

  @Test
  public void expectedTypeExtension9() {
    typeCheckModule("""
      \\record A (x y : Nat)
      \\record B \\extends A | x => 0
      \\func f (a : A 1) : A 1 => \\new B { | A => a }
      """, 2);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("A.x"))), typeMismatchError());
  }

  @Test
  public void universesTest() {
    typeCheckModule(
      "\\record C (A : \\Type) (a : A)\n" +
      "\\func f (c : C \\levels 1 1 Nat) : C \\levels 0 1 => c");
  }

  @Test
  public void universesTestError() {
    typeCheckModule(
      "\\record C (A : \\Type) (a : A)\n" +
      "\\func f (c : C \\levels 1 1 \\Set0) : C \\levels 0 1 => c", 1);
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
      "\\func f (c : C \\Type5 \\Type3) : C \\Type5 \\Type4 => c");
  }

  @Test
  public void comparisonTest3() {
    typeCheckModule(
      "\\record C (A : Nat -> \\Type)\n" +
      "\\func f (c : C (\\lam _ => \\Type4)) : C (\\lam _ => \\Type5) => c");
  }

  @Test
  public void extensionParameterError() {
    typeCheckModule("""
      \\class C
      \\func f {c : C} => 0
      \\class D (X : \\Type) \\extends C
        | g : f = f
      """, 2);
  }

  @Test
  public void extensionParameterTest() {
    typeCheckModule("""
      \\class C
      \\func f {c : C} => 0
      \\class D \\noclassifying (X : \\Type) \\extends C
        | g : f = f
      """);
  }

  @Test
  public void extensionParameterError2() {
    typeCheckModule("""
      \\class C (X : \\Type)
      \\class D \\extends C
        | x : X
      \\func test (d : D) => x
      """, 1);
  }

  @Test
  public void extensionParameterTest2() {
    typeCheckModule("""
      \\class C (X : \\Type)
        | x : X
      \\class D \\noclassifying \\extends C
        | y : X
      \\func test (d : D) => d.x = {d.X} y
      """);
  }

  @Test
  public void recursionTest() {
    typeCheckModule("""
      \\record B
      \\record R \\extends S
        \\where
          \\record S \\extends B
      """);
  }

  @Test
  public void recursionTest2() {
    typeCheckModule("""
      \\record B
      \\record E \\extends R
        \\where
          \\record R \\extends S
            \\where
              \\record S \\extends B
      """);
  }
}
