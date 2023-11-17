package org.arend.typechecking.typeclass;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.DataDefinition;
import org.arend.core.subst.Levels;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.ExpressionFactory.DataCall;
import static org.arend.Matchers.*;
import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

public class TypeClassesGlobalTest extends TypeCheckingTestCase {
  @Test
  public void inferInstance() {
    typeCheckModule("""
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }
      \\instance Nat-X : X | A => Nat | B => \\lam n => Nat
      \\func f => B 0""");
  }

  @Test
  public void instanceClassWithArg() {
    typeCheckModule("""
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }
      \\instance Nat-X : X Nat | B => \\lam n => Nat
      \\func f => B 0""");
  }

  @Test
  public void incorrectInstance() {
    typeCheckModule("""
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }
      \\func f : \\Type1 => Nat
      \\instance Nat-X : X | A => f | B => \\lam n => Nat""", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void differentInstances() {
    typeCheckModule("""
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }
      \\instance Nat-X : X | A => Nat | B => \\lam n => Nat
      \\instance I-X : X | A => I | B => \\lam n => Nat -> Nat
      \\func f => B 0
      \\func g => B left""");
  }

  @Test
  public void localInstance() {
    typeCheckModule("""
      \\class X (A : \\Type0) {
        | B : A -> \\Set0
      }
      \\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat
      \\func f (y : X { | A => Nat }) => B 0
      \\func test : Nat = Nat => path (\\lam _ => f (\\new X { | A => Nat | B => \\lam _ => Nat }))""");
  }

  @Test
  public void transitiveInferInstance() {
    typeCheckModule("""
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }
      \\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat
      \\func f {A : \\Type0} {y : X { | A => A } } (a : A) => B a
      \\func g => f 0""");
  }

  @Test
  public void transitiveInferInstance2() {
    typeCheckModule("""
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }
      \\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat
      \\func f {x : X} (a : x.A) => B a
      \\func g => f 0""");
  }

  @Test
  public void mutuallyRecursiveInstance() {
    typeCheckModule("""
      \\instance Nat-X : X | A => Nat | B => \\lam _ => Nat
      \\data D | c
      \\func g {x : X { | A => Nat }} => \\Prop
      \\func f : \\Set0 => g
      \\instance D-X : X | A => D | B => \\lam _ => f
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }""");
  }

  @Test
  public void mutuallyRecursiveInstance2() {
    typeCheckModule("""
      \\instance Nat-X : X | A => Nat | B => \\lam _ => Nat
      \\data D | c
      \\func g {x : X { | A => Nat }} => \\Prop
      \\instance D-X : X | A => D | B => \\lam _ => f
      \\func f : \\Set0 => g
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }""");
  }

  // We do not check for duplicate global instances currently
  @Test
  public void duplicateInstance() {
    typeCheckModule("""
      \\class X (A : \\Type0) {
        | B : A -> \\Type0
      }
      \\data D
      \\instance D-X : X | A => D | B => \\lam n => D
      \\instance D-Y : X | A => D | B => \\lam n => D -> D""");
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
    typeCheckModule("""
      \\class A { | n : Nat }
      \\instance a0 : A | n => 0
      \\instance a1 : A | n => 1
      \\func f : n = n => path (\\lam _ => 0)""");
  }

  @Test
  public void checkClassifyingExpressionArguments() {
    typeCheckModule("""
      \\data Data (A : \\Set)
      \\data D
      \\data D'
      \\class B (X : \\Set) { | foo : X -> X }
      \\instance B-inst : B (Data D) | foo => \\lam x => x
      \\func f (x : Data D') => foo x""", 1);
    assertThatErrorsAre(argInferenceError());
  }

  @Test
  public void classifyingFieldIsNotADefCall() {
    typeCheckModule(
      "\\class B (n : Nat)\n" +
      "\\instance B-inst {x : Nat} : B x", 1);
  }

  @Test
  public void superClassInstance() {
    typeCheckModule("""
      \\class A { | x : Nat }
      \\class B \\extends A
      \\instance B-inst : B | x => 0
      \\func f => x""");
  }

  @Test
  public void superClassWithClassifyingFieldInstance() {
    typeCheckModule("""
      \\class A (C : \\Set) { | c : C }
      \\class B \\extends A
      \\instance B-inst : B Nat | c => 0
      \\func f : Nat => c""");
  }

  @Test
  public void superClassWithClassifyingFieldNoInstance() {
    typeCheckModule("""
      \\class A (C : \\Set) { | c : C }
      \\class B \\extends A
      \\data Nat'
      \\instance B-inst : B Nat | c => 0
      \\func f : Nat' => c""", 1);
    assertThatErrorsAre(instanceInference(get("A"), DataCall((DataDefinition) getDefinition("Nat'"), Levels.EMPTY)));
  }

  @Test
  public void instanceProp() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C }
      \\data D : \\Prop
      \\instance aaa : A \\Prop | c => D
      \\func test : \\Prop => c""");
  }

  @Test
  public void instancePropError() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C }
      \\data D : \\Prop
      \\instance aaa : A \\Prop | c => D
      \\func test : \\Set => c""", 1);
    assertThatErrorsAre(argInferenceError());
  }

  @Test
  public void instancePropError2() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C }
      \\data D : \\Prop
      \\instance aaa : A \\Prop | c => D
      \\func test : \\Type => c""", 1);
    assertThatErrorsAre(argInferenceError());
  }

  @Test
  public void instanceSet() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C }
      \\instance a : A \\Set | c => Nat
      \\func f1 : \\Set => c
      \\func f2 : \\Type => c""");
  }

  @Test
  public void instanceType() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C }
      \\data D | con \\Set0
      \\instance a : A \\Type1 | c => D
      \\func f : \\1-Type => c""");
  }

  @Test
  public void instanceTypeError() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C }
      \\instance a : A \\Set | c => Nat
      \\func f : \\Prop => c""", 1);
    assertThatErrorsAre(argInferenceError());
  }

  @Test
  public void instanceTypeError2() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C }
      \\data D | con \\Set0
      \\instance a : A \\0-Type1 | c => D""", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void instanceTypeError3() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C }
      \\data D | con \\Set0
      \\instance a : A \\1-Type1 | c => D
      \\func f : \\1-Type0 => c""", 1);
    assertThatErrorsAre(argInferenceError());
  }

  @Test
  public void instanceTypeCheckTest() {
    typeCheckModule("""
      \\class A (C : \\Type) { | c : C | n : Nat }
      \\instance a : A \\Set | c => Nat | n => 0
      \\func f {c : A { | C => \\Set | n => 1 }} => 2
      \\func g => f""", 1);
    assertThatErrorsAre(instanceInference(get("A")));
  }

  @Test
  public void classifyingFieldLambda() {
    typeCheckModule("""
      \\class B (F : \\Set -> \\Set) | foo : F Nat | bar : F Nat -> Nat
      \\data Maybe (A : \\Type) | nothing | just A
      \\func fromMaybe {A : \\Type} (a : A) (m : Maybe A) : A \\elim m
        | nothing => a
        | just a' => a'
      \\instance B-inst : B Maybe | foo => just 3 | bar => fromMaybe 7
      \\func test1 => fromMaybe 4 foo
      \\func test2 => bar (just 5)
      \\func test3 => \\let x : Maybe Nat => foo \\in bar x""");
  }

  @Test
  public void classifyingFieldLambdaError() {
    typeCheckModule("""
      \\class B (F : \\Set -> \\Set) | foo : F Nat | bar : F Nat -> Nat
      \\data Maybe (A : \\Type) | nothing | just A
      \\func fromMaybe {A : \\Type} (a : A) (m : Maybe A) : A \\elim m
        | nothing => a
        | just a' => a'
      \\instance B-inst : B Maybe | foo => just 3 | bar => fromMaybe 7
      \\func test => bar (just (\\lam (x : Nat) => x))""", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void classifyingFieldLambdaClass() {
    typeCheckModule("""
      \\class B (F : \\Set -> \\Set) | foo : F Nat | bar : F Nat -> Nat
      \\record R {A : \\Type} | rrr : A
      \\func proj {A : \\Type} (r : R {A}) => r.rrr
      \\instance B-inst : B (\\lam A => R {A}) | foo => \\new R 3 | bar => proj
      \\func test1 => proj foo
      \\func test2 => bar (\\new R 5)
      \\func test3 => \\let x : R {Nat} => foo \\in bar x""");
  }

  @Test
  public void whereInstance() {
    typeCheckModule("""
      \\class X (A : \\Type) | xxx : A
      \\instance IntX : X Nat | xxx => 0
      \\instance NatX : X Int | xxx => foo
        \\where \\func foo => pos xxx""");
  }

  @Test
  public void explicitInstance() {
    typeCheckModule("""
      \\class X (A : \\Type) | xxx : A
      \\instance NatX : X Nat | xxx => 0
      \\instance IntX : X Int | xxx => foo
      \\func foo => pos NatX.xxx""");
  }

  @Test
  public void instanceSyntax() {
    typeCheckModule("""
      \\class C (A : \\Type) | a : A
      \\instance NatC : C Nat { | a => 0 }
      \\func f : Nat => a""");
  }

  @Test
  public void whereTest() {
    typeCheckModule("""
      \\class C | x : Nat
      \\func f {c : C} => x {c}
      \\func g : Nat => f
        \\where \\instance ccc : C | x => 1""");
  }

  @Test
  public void nonClassTest() {
    typeCheckModule("""
      \\class C | x : Nat
      \\func f {c : (C,C).1} => x {c}
      \\func g : Nat => f
        \\where \\instance ccc : C | x => 1""", 1);
    assertThatErrorsAre(argInferenceError());
    assertThatErrorsAre(not(instanceInference(get("C"))));
  }

  @Test
  public void nonClassWithFieldTest() {
    typeCheckModule("""
      \\class C (X : \\Type) | x : X
      \\func f {c : (C Nat, C Nat).1} => x {c}
      \\func g : Nat => f
        \\where \\instance ccc : C Nat | x => 1""", 1);
    assertThatErrorsAre(argInferenceError());
    assertThatErrorsAre(not(instanceInference(get("C"))));
    assertThatErrorsAre(not(instanceInference(get("C"), Nat())));
  }

  @Test
  public void explicitImplicitArgument() {
    typeCheckModule("""
      \\class C (X : \\Type) | f : X -> X
      \\instance C_Nat : C Nat | f => suc
      \\func g => f {_} 1""");
  }

  @Test
  public void classifyingFieldImpl() {
    typeCheckModule("""
      \\class C (X : \\hType)
      \\class D \\extends C | X => Nat -> Nat
      \\instance ddd : D""");
  }

  @Test
  public void changeClassifying() {
    typeCheckModule("""
      \\class C (A : \\Type)
      \\class D (\\classifying B : \\Type) \\extends C | b : B
      \\instance inst1 : D Int Nat | b => 1
      \\instance inst2 : D Int (\\Sigma Nat Nat) | b => (2,2)
      \\func test1 : Nat => b
      \\func test2 : \\Sigma Nat Nat => b""");
    ClassDefinition classD = (ClassDefinition) getDefinition("D");
    ClassField fieldB = (ClassField) getDefinition("D.B");
    assertEquals(classD.getClassifyingField(), fieldB);
  }

  @Test
  public void implementClassifying() {
    typeCheckModule("""
      \\class C (A : \\Set)
      \\class D (\\classifying B : \\Set) \\extends C | b : B
      \\class E \\extends D | B => Nat | a : A
      \\instance inst1 : E Nat 0 | a => 1
      \\instance inst2 : E (\\Sigma Nat Nat) 0 | a => (2,2)
      \\func test1 : Nat => a
      \\func test2 : \\Sigma Nat Nat => a""");
    ClassDefinition classE = (ClassDefinition) getDefinition("E");
    ClassField fieldA = (ClassField) getDefinition("E.A");
    assertEquals(classE.getClassifyingField(), fieldA);
  }

  @Test
  public void coClauseTest() {
    typeCheckModule("""
      \\class C (X : \\Type) | x : X
      \\record R | f : Nat -> Nat
      \\func test : R \\cowith
        | f (n : Nat) : Nat \\with {
          | 0 => x
          | suc n => n
      }
        \\where \\instance inst => \\new C Nat 0""");
  }

  @Test
  public void instanceWhereTest() {
    typeCheckModule("""
      \\class C | f : Nat
      \\instance c : C
        | f => 0
        \\where
          \\func test => f""", 1);
  }

  @Test
  public void defaultTest() {
    typeCheckModule("""
      \\class C | x : Nat
      \\instance c : C | x => 3
      \\record R (f : Nat -> Nat)
      \\record S \\extends R {
        \\default f (n : Nat) : Nat => x
      }""");
  }

  @Test
  public void mutualTest() {
    typeCheckModule("""
      \\class C (A : \\Type)
       | f : A -> A
      \\module M1 \\where {
        \\open M2
        \\instance inst1 : C Nat
          | f x => x
      }
      \\module M2 \\where {
        \\open M1
        \\instance inst2 : C Nat
          | f x => x
      }
      """, 1);
  }
}
