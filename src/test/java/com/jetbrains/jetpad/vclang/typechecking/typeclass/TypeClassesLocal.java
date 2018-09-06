package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.instanceInference;

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
      "\\func f (a : A) : n = n => path (\\lam _ => a.n)", 2);
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
    assertThatErrorsAre(instanceInference(getDefinition("A")));
  }

  @Test
  public void instanceTypeCheckTest() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C | n : Nat }\n" +
      "\\func id (A : \\Type) => A\n" +
      "\\func f {a : A \\1-Type} : \\Set => id c", 1);
  }
}
