package org.arend.typechecking.typeclass;

import org.arend.Matchers;
import org.arend.core.definition.DataDefinition;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.arend.ExpressionFactory.DataCall;

public class TypeClassesLocal extends TypeCheckingTestCase {
  @Test
  public void inferVar() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (x : X) (a : x.A) => B a");
  }

  @Test
  public void inferVar2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (A' : \\Type0) (x : X { | A => A' }) (a : A') => B a");
  }

  @Test
  public void inferVarGlobalType() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (x : X { | A => Nat }) => B 0");
  }

  @Test
  public void inferVarDuplicateTele() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (x y : X) (a : y.A) => B a");
  }

  @Test
  public void inferVarDuplicateTele2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (A : \\Type0) (x y : X { | A => A }) (a : y.A) => 0");
    // assertThatErrorsAre(duplicateInstanceError());
  }

  @Test
  public void inferVarDuplicate() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (x : X) (a : x.A) {y : X { | A => x.A } } => 0");
    // assertThatErrorsAre(duplicateInstanceError());
  }

  @Test
  public void inferVarDuplicate2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (A' : \\Type0) (x : X { | A => A' }) {y : X { | A => A' } } (a : A') => 0");
    // assertThatErrorsAre(duplicateInstanceError());
  }

  @Test
  public void inferVar3() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (A' : \\Type0) {y : X { | A => A' -> A' } } (a : A') (x : X { | A => A' }) => B a");
  }

  @Test
  public void inferVarFromType() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (x : X) : x.A => a");
  }

  @Test
  public void inferVarDuplicateFromType() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (x : X) (y : X { | A => x.A }) : x.A => a");
    // assertThatErrorsAre(duplicateInstanceError());
  }

  @Test
  public void inferVarFromType2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (A' : \\Type0) (x : X { | A => A' }) : A' => a");
  }

  @Test
  public void inferVarDuplicateFromType2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (A' : \\Type0) (y : X { | A => A' }) (x : X { | A => A' }) : A' => a");
    // assertThatErrorsAre(duplicateInstanceError());
  }

  @Test
  public void inferVarFromType3() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (x : X) (y : X { | A => x.A -> x.A }) : x.A -> x.A => a");
  }

  @Test
  public void withoutClassifyingField() {
    typeCheckModule(
      "\\class A { | n : Nat }\n" +
      "\\func f (a : A) : n = n => path (\\lam _ => a.n)");
  }

  @Test
  public void recordWithoutClassifyingField() {
    typeCheckModule(
      "\\record A { | n : Nat }\n" +
      "\\func f (a : A) : n = n => path (\\lam _ => a.n)");
  }

  @Test
  public void recordWithoutClassifyingField2() {
    typeCheckModule(
      "\\record A { | n : Nat }\n" +
      "\\func f (a : A) : n = n => idp", 1);
  }

  @Test
  public void superClassInstance() {
    typeCheckModule(
      "\\class A { | x : Nat }\n" +
      "\\class B \\extends A\n" +
      "\\func f {b : B} => x");
  }

  @Test
  public void superClassWithClassifyingFieldInstance() {
    typeCheckModule(
      "\\class A (C : \\Set) { | c : C }\n" +
      "\\class B \\extends A\n" +
      "\\func f {b : B Nat} : Nat => c");
  }

  @Test
  public void superClassWithClassifyingFieldNoInstance() {
    typeCheckModule(
      "\\class A (C : \\Set) { | c : C }\n" +
      "\\class B \\extends A\n" +
      "\\data Nat'\n" +
      "\\func f {b : B Nat} : Nat' => c", 1);
    assertThatErrorsAre(Matchers.instanceInference(get("A"), DataCall((DataDefinition) getDefinition("Nat'"), Sort.STD)));
  }

  @Test
  public void instanceTypeCheckTest() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C | n : Nat }\n" +
      "\\func id (A : \\Type) => A\n" +
      "\\func f {a : A \\1-Type} : \\Set => id c", 1);
  }

  @Test
  public void classInRecordParameter() {
    typeCheckModule(
      "\\class C (X : \\Type) | x : X\n" +
      "\\record R {c : C} (y : c.X) (p : y = x) | q : x = y");
  }

  @Test
  public void classWithArgumentInRecordParameter() {
    typeCheckModule(
      "\\class C (X : \\Type) | f : X -> Nat\n" +
      "\\record R {c : C Nat} (p : f 0 = 0) | q : f 1 = f 2");
  }

  @Test
  public void classWithoutClassifyingFieldInRecordParameter() {
    typeCheckModule(
      "\\class C | n : Nat\n" +
      "\\record R {c : C} (p : n = n) | q : n = n");
  }

  @Test
  public void classInRecordParameterAndImpl() {
    typeCheckModule(
      "\\class C (X : \\Type) | x : X\n" +
      "\\record A | n : Nat\n" +
      "\\record B {c : C Nat} \\extends A | n => x");
  }

  @Test
  public void binaryAndNullaryTest() {
    typeCheckModule(
      "\\class C (X : \\Type) | x0 : X | \\infixr 4 * : X -> X -> X\n" +
      "\\func f {c : C} (x : c) => x0 * x");
  }

  @Test
  public void recursiveData() {
    typeCheckModule(
      "\\class C | foo : Nat\n" +
      "\\data D {c : C}\n" +
      "  | con D");
  }

  @Test
  public void letTest() {
    typeCheckModule(
      "\\class C | field : Nat\n" +
      "\\func test => \\let inst => \\new C 0 \\in field");
  }

  @Test
  public void letWithClassifying() {
    typeCheckModule(
      "\\class C {A : \\Type} | field : A\n" +
      "\\func test : Nat => \\let inst => \\new C 0 \\in field");
  }

  @Test
  public void letWithClassifyingError() {
    typeCheckModule(
      "\\class C {A : \\Type} | field : A\n" +
      "\\func test : Nat => \\let inst => \\new C (pos 0) \\in field", 1);
  }

  @Test
  public void fieldInstanceInDynamicDefinition() {
    typeCheckModule(
      "\\class C | foo : Nat\n" +
      "\\class D (c : C) {\n" +
      "  \\func test => foo\n" +
      "}");
  }

  @Test
  public void fieldInstance() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
      "  | a : A\n" +
      "\\record R (c : C Nat) {\n" +
      "  | f : suc a = 0\n" +
      "  \\func g : Nat => a\n" +
      "}");
  }

  @Ignore
  @Test
  public void superFieldInstance() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
      "  | a : A\n" +
      "\\record R (c : C Nat)\n" +
      "\\record S \\extends R {\n" +
      "  | f : suc a = 0\n" +
      "  \\func g : Nat => a\n" +
      "}");
  }

  @Test
  public void twoInstances() {
    typeCheckModule(
      "\\class B (A : \\Set)\n" +
      "\\class C \\extends B\n" +
      "  | c : A\n" +
      "\\class D \\extends B\n" +
      "  | d : A\n" +
      "\\func f {d : D} (x : d) => x\n" +
      "\\func test (X : C) (Y : D X) (y : Y) => f y");
  }

  @Test
  public void twoInstances2() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
      "  | c : A\n" +
      "\\class D (A' : \\Set)\n" +
      "  | d : A'\n" +
      "\\func f {d : D} (x : d) => x\n" +
      "\\func test (X : C) (Y : D X) (y : Y) => f y");
  }

  @Test
  public void twoInstances3() {
    typeCheckModule(
      "\\class B (A : \\Set)\n" +
      "\\class C \\extends B\n" +
      "  | c : A\n" +
      "\\class D \\extends B\n" +
      "  | d : A\n" +
      "\\func f {d : D} (x : d) => x\n" +
      "\\func test {A : \\Set} (X : C A) (Y : D A) (y : Y) => f y");
  }
}
