package org.arend.typechecking.typeclass;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.DataDefinition;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.ExpressionFactory.DataCall;
import static org.arend.Matchers.*;
import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

public class TypeClassesGlobal extends TypeCheckingTestCase {
  @Test
  public void inferInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat\n" +
      "\\func f => B 0");
  }

  @Test
  public void instanceClassWithArg() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X Nat | B => \\lam n => Nat\n" +
      "\\func f => B 0");
  }

  @Test
  public void incorrectInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\func f : \\Type1 => Nat\n" +
      "\\instance Nat-X : X | A => f | B => \\lam n => Nat", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void differentInstances() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat\n" +
      "\\instance I-X : X | A => I | B => \\lam n => Nat -> Nat\n" +
      "\\func f => B 0\n" +
      "\\func g => B left");
  }

  @Test
  public void localInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Set0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f (y : X { | A => Nat }) => B 0\n" +
      "\\func test : Nat = Nat => path (\\lam _ => f (\\new X { | A => Nat | B => \\lam _ => Nat }))");
  }

  @Test
  public void transitiveInferInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f {A : \\Type0} {y : X { | A => A } } (a : A) => B a\n" +
      "\\func g => f 0");
  }

  @Test
  public void transitiveInferInstance2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f {x : X} (a : x.A) => B a\n" +
      "\\func g => f 0");
  }

  @Test
  public void mutuallyRecursiveInstance() {
    typeCheckModule(
      "\\instance Nat-X : X | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\func g {x : X { | A => Nat }} => \\Prop\n" +
      "\\func f : \\Set0 => g\n" +
      "\\instance D-X : X | A => D | B => \\lam _ => f\n" +
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}");
  }

  @Test
  public void mutuallyRecursiveInstanceError() {
    typeCheckModule(
      "\\instance Nat-X : X | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\func g {x : X { | A => Nat }} => \\Prop\n" +
      "\\instance D-X : X | A => D | B => \\lam _ => f\n" +
      "\\func f : \\Set0 => g\n" +
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}", 2);
  }

  // We do not check for duplicate global instances currently
  @Test
  public void duplicateInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\data D\n" +
      "\\instance D-X : X | A => D | B => \\lam n => D\n" +
      "\\instance D-Y : X | A => D | B => \\lam n => D -> D");
    // assertThatErrorsAre(duplicateInstanceError());
  }

  @Test
  public void withoutClassifyingFieldError() {
    typeCheckModule(
      "\\class A { | n : Nat }\n" +
      "\\func f => n", 1);
    assertThatErrorsAre(instanceInference(get("A")));
  }

  @Test
  public void withoutClassifyingField() {
    typeCheckModule(
      "\\class A { | n : Nat }\n" +
      "\\instance a0 : A | n => 0\n" +
      "\\instance a1 : A | n => 1\n" +
      "\\func f : n = n => path (\\lam _ => 0)");
  }

  @Test
  public void checkClassifyingExpressionArguments() {
    typeCheckModule(
      "\\data Data (A : \\Set)\n" +
      "\\data D\n" +
      "\\data D'\n" +
      "\\class B (X : \\Set) { | foo : X -> X }\n" +
      "\\instance B-inst : B (Data D) | foo => \\lam x => x\n" +
      "\\func f (x : Data D') => foo x", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void classifyingFieldIsNotADefCall() {
    typeCheckModule(
      "\\class B (n : Nat)\n" +
      "\\instance B-inst {x : Nat} : B x", 1);
  }

  @Test
  public void superClassInstance() {
    typeCheckModule(
      "\\class A { | x : Nat }\n" +
      "\\class B \\extends A\n" +
      "\\instance B-inst : B | x => 0\n" +
      "\\func f => x");
  }

  @Test
  public void superClassWithClassifyingFieldInstance() {
    typeCheckModule(
      "\\class A (C : \\Set) { | c : C }\n" +
      "\\class B \\extends A\n" +
      "\\instance B-inst : B Nat | c => 0\n" +
      "\\func f : Nat => c");
  }

  @Test
  public void superClassWithClassifyingFieldNoInstance() {
    typeCheckModule(
      "\\class A (C : \\Set) { | c : C }\n" +
      "\\class B \\extends A\n" +
      "\\data Nat'\n" +
      "\\instance B-inst : B Nat | c => 0\n" +
      "\\func f : Nat' => c", 1);
    assertThatErrorsAre(instanceInference(get("A"), DataCall((DataDefinition) getDefinition("Nat'"), Sort.STD)));
  }

  @Test
  public void instanceProp() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\data D : \\Prop\n" +
      "\\instance aaa : A \\Prop | c => D\n" +
      "\\func f1 : \\Prop => c\n" +
      "\\func f2 : \\Set => c\n" +
      "\\func f3 : \\Type => c");
  }

  @Test
  public void instanceSet() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\instance a : A \\Set | c => Nat\n" +
      "\\func f1 : \\Set => c\n" +
      "\\func f2 : \\Type => c");
  }

  @Test
  public void instanceType() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\data D | con \\Set0\n" +
      "\\instance a : A \\Type1 | c => D\n" +
      "\\func f : \\1-Type => c");
  }

  @Test
  public void instanceTypeError() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\instance a : A \\Set | c => Nat\n" +
      "\\func f : \\Prop => c", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void instanceTypeError2() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\data D | con \\Set0\n" +
      "\\instance a : A \\0-Type1 | c => D", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void instanceTypeError3() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\data D | con \\Set0\n" +
      "\\instance a : A \\1-Type1 | c => D\n" +
      "\\func f : \\1-Type0 => c", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void instanceTypeCheckTest() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C | n : Nat }\n" +
      "\\instance a : A \\Set | c => Nat | n => 0\n" +
      "\\func f {c : A { | C => \\Set | n => 1 }} => 2\n" +
      "\\func g => f", 1);
    assertThatErrorsAre(instanceInference(get("A")));
  }

  @Test
  public void classifyingFieldLambda() {
    typeCheckModule(
      "\\class B (F : \\Set -> \\Set) | foo : F Nat | bar : F Nat -> Nat\n" +
      "\\data Maybe (A : \\Type) | nothing | just A\n" +
      "\\func fromMaybe {A : \\Type} (a : A) (m : Maybe A) : A \\elim m\n" +
      "  | nothing => a\n" +
      "  | just a' => a'\n" +
      "\\instance B-inst : B Maybe | foo => just 3 | bar => fromMaybe 7\n" +
      "\\func test1 => fromMaybe 4 foo\n" +
      "\\func test2 => bar (just 5)\n" +
      "\\func test3 => \\let x : Maybe Nat => foo \\in bar x");
  }

  @Test
  public void classifyingFieldLambdaError() {
    typeCheckModule(
      "\\class B (F : \\Set -> \\Set) | foo : F Nat | bar : F Nat -> Nat\n" +
      "\\data Maybe (A : \\Type) | nothing | just A\n" +
      "\\func fromMaybe {A : \\Type} (a : A) (m : Maybe A) : A \\elim m\n" +
      "  | nothing => a\n" +
      "  | just a' => a'\n" +
      "\\instance B-inst : B Maybe | foo => just 3 | bar => fromMaybe 7\n" +
      "\\func test => bar (just (\\lam (x : Nat) => x))", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void classifyingFieldLambdaClass() {
    typeCheckModule(
      "\\class B (F : \\Set -> \\Set) | foo : F Nat | bar : F Nat -> Nat\n" +
      "\\record R {A : \\Type} | rrr : A\n" +
      "\\func proj {A : \\Type} (r : R {A}) => r.rrr\n" +
      "\\instance B-inst : B (\\lam A => R {A}) | foo => \\new R 3 | bar => proj\n" +
      "\\func test1 => proj foo\n" +
      "\\func test2 => bar (\\new R 5)\n" +
      "\\func test3 => \\let x : R {Nat} => foo \\in bar x");
  }

  @Test
  public void whereInstance() {
    typeCheckModule(
      "\\class X (A : \\Type) | xxx : A\n" +
      "\\instance IntX : X Nat | xxx => 0\n" +
      "\\instance NatX : X Int | xxx => foo\n" +
      "  \\where \\func foo => pos xxx");
  }

  @Test
  public void explicitInstance() {
    typeCheckModule(
      "\\class X (A : \\Type) | xxx : A\n" +
      "\\instance NatX : X Nat | xxx => 0\n" +
      "\\instance IntX : X Int | xxx => foo\n" +
      "\\func foo => pos NatX.xxx");
  }

  @Test
  public void instanceSyntax() {
    typeCheckModule(
      "\\class C (A : \\Type) | a : A\n" +
      "\\instance NatC : C Nat { | a => 0 }\n" +
      "\\func f : Nat => a");
  }

  @Test
  public void whereTest() {
    typeCheckModule(
      "\\class C | x : Nat\n" +
      "\\func f {c : C} => x {c}\n" +
      "\\func g : Nat => f\n" +
      "  \\where \\instance ccc : C | x => 1");
  }

  @Test
  public void nonClassTest() {
    typeCheckModule(
      "\\class C | x : Nat\n" +
      "\\func f {c : (C,C).1} => x {c}\n" +
      "\\func g : Nat => f\n" +
      "  \\where \\instance ccc : C | x => 1", 1);
    assertThatErrorsAre(argInferenceError());
    assertThatErrorsAre(not(instanceInference(get("C"))));
  }

  @Test
  public void nonClassWithFieldTest() {
    typeCheckModule(
      "\\class C (X : \\Type) | x : X\n" +
      "\\func f {c : (C Nat, C Nat).1} => x {c}\n" +
      "\\func g : Nat => f\n" +
      "  \\where \\instance ccc : C Nat | x => 1", 1);
    assertThatErrorsAre(argInferenceError());
    assertThatErrorsAre(not(instanceInference(get("C"))));
    assertThatErrorsAre(not(instanceInference(get("C"), Nat())));
  }

  @Test
  public void explicitImplicitArgument() {
    typeCheckModule(
      "\\class C (X : \\Type) | f : X -> X\n" +
      "\\instance C_Nat : C Nat | f => suc\n" +
      "\\func g => f {_} 1");
  }

  @Test
  public void classifyingFieldImpl() {
    typeCheckModule(
      "\\class C (X : \\hType)\n" +
      "\\class D \\extends C | X => Nat -> Nat\n" +
      "\\instance ddd : D");
  }

  @Test
  public void changeClassifying() {
    typeCheckModule(
      "\\class C (A : \\Type)\n" +
        "\\class D (\\classifying B : \\Type) \\extends C | b : B\n" +
        "\\instance inst1 : D Int Nat | b => 1\n" +
        "\\instance inst2 : D Int (\\Sigma Nat Nat) | b => (2,2)\n" +
        "\\func test1 : Nat => b\n" +
        "\\func test2 : \\Sigma Nat Nat => b");
    ClassDefinition classD = (ClassDefinition) getDefinition("D");
    ClassField fieldB = (ClassField) getDefinition("D.B");
    assertEquals(classD.getClassifyingField(), fieldB);
  }

  @Test
  public void implementClassifying() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
        "\\class D (\\classifying B : \\Set) \\extends C | b : B\n" +
        "\\class E \\extends D | B => Nat | a : A\n" +
        "\\instance inst1 : E Nat 0 | a => 1\n" +
        "\\instance inst2 : E (\\Sigma Nat Nat) 0 | a => (2,2)\n" +
        "\\func test1 : Nat => a\n" +
        "\\func test2 : \\Sigma Nat Nat => a");
    ClassDefinition classE = (ClassDefinition) getDefinition("E");
    ClassField fieldA = (ClassField) getDefinition("E.A");
    assertEquals(classE.getClassifyingField(), fieldA);
  }
}
