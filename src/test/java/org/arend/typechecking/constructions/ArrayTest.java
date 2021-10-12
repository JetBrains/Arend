package org.arend.typechecking.constructions;

import org.arend.Matchers;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.NotEqualExpressionsError;
import org.arend.util.SingletonList;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.*;

public class ArrayTest extends TypeCheckingTestCase {
  @Test
  public void resolveTest() {
    typeCheckDef("\\func fold_<=_meet1 (l : Array Nat) => l.len");
  }

  @Test
  public void classExt() {
    typeCheckModule(
      "\\func test1 => \\new Array Nat 1 (\\lam _ => 3)\n" +
      "\\func test2 : Array Nat \\cowith\n" +
      "  | len => 3\n" +
      "  | at _ => 1");
    Sort sort = Sort.STD.max(Sort.SET0);
    assertTrue(((ClassCallExpression) ((FunctionDefinition) getDefinition("test1")).getResultType()).isImplemented(Prelude.ARRAY_AT));
    assertEquals(Sort.PROP, ((ClassCallExpression) ((FunctionDefinition) getDefinition("test1")).getResultType()).getSort());
    assertTrue(((ClassCallExpression) ((FunctionDefinition) getDefinition("test2")).getResultType()).isImplemented(Prelude.ARRAY_AT));
    assertEquals(Sort.PROP, ((ClassCallExpression) ((FunctionDefinition) getDefinition("test2")).getResultType()).getSort());
    assertFalse(((ClassCallExpression) Prelude.EMPTY_ARRAY.getResultType()).isImplemented(Prelude.ARRAY_AT));
    assertEquals(sort, ((ClassCallExpression) Prelude.EMPTY_ARRAY.getResultType()).getSort());
    assertFalse(((ClassCallExpression) Prelude.ARRAY_CONS.getResultType()).isImplemented(Prelude.ARRAY_AT));
    assertEquals(sort, ((ClassCallExpression) Prelude.ARRAY_CONS.getResultType()).getSort());
  }

  @Test
  public void consTest() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func test (n : Nat) (x : Array Nat n) : Array Nat (suc n) => n :: x");
    assertTrue(def.getBody() instanceof ArrayExpression);
    assertEquals(Sort.SET0, ((ClassCallExpression) def.getResultType()).getSort());
  }

  @Test
  public void consTest1() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func foo => 1 :: nil");
    assertTrue(def.getBody() instanceof ArrayExpression);
    assertEquals(Sort.SET0, ((ClassCallExpression) def.getResultType()).getSort());
  }

  @Test
  public void consTest2() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func foo => 1 :: 2 :: nil");
    assertTrue(def.getBody() instanceof ArrayExpression);
    assertEquals(Sort.SET0, ((ClassCallExpression) def.getResultType()).getSort());
  }

  @Test
  public void consTest3() {
    typeCheckDef("\\func test => 7 ::");
  }

  @Test
  public void indexTest() {
    typeCheckModule(
      "\\open DArray(!!)\n" +
      "\\func array : Array Nat 2 => 14 :: 22 :: nil\n" +
      "\\lemma test1 : array.at 0 = 14 => idp\n" +
      "\\lemma test2 : array.at 1 = 22 => idp\n" +
      "\\lemma test3 : array !! 0 = 14 => idp\n" +
      "\\lemma test4 : array !! 1 = 22 => idp\n" +
      "\\func test5 {A : \\Type} (a : Array A) (i : Fin a.len) : a.at i = a !! i => idp");
  }

  @Test
  public void extendsTest() {
    resolveNamesDef("\\record R \\extends Array", 1);
    assertThatErrorsAre(Matchers.notInScope("Array"));
  }

  @Test
  public void nilEtaTest() {
    typeCheckModule(
      "\\lemma test1 (a b : Array Nat 0) : a = b => idp\n" +
      "\\func test2 (a : DArray { | len => 0 }) : a = nil => idp\n" +
      "\\func test3 (a : DArray { | len => 0 }) : nil = a => idp");
  }

  @Test
  public void nilEtaError() {
    typeCheckDef("\\func test (a b : DArray { | len => 0 }) : a = b => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void newConsTest() {
    typeCheckModule(
      "\\lemma test1 : (\\new Array Nat 2 (\\case __ \\with { | 0 => 5 | 1 => 7 })) = 5 :: 7 :: nil => idp\n" +
      "\\lemma test2 : (\\new Array Nat 3 (\\case __ \\with { | 0 => 5 | suc i => \\case i \\with { | 0 => 7 | 1 => 12 } })) = (\\new Array Nat 3 (\\case __ \\with { | 0 => 5 | 1 => 7 | 2 => 12 })) => idp");
  }

  @Test
  public void cowithIndexTest() {
    typeCheckModule(
      "\\func f (a : Array Nat) : Array Nat a.len (\\lam _ => 7) \\cowith\n" +
      "\\lemma test (a : Array Nat) (i : Fin a.len) : f a DArray.!! i = 7 => idp");
  }

  @Test
  public void consEtaTest() {
    typeCheckModule(
      "\\func map {A B : \\Type} (f : A -> B) (as : Array A) : Array B as.len (\\lam i => f (as i)) \\cowith\n" +
      "\\func test {A B : \\Type} (f : A -> B) {a : A} (as : Array A) : map f (a :: as) = f a :: map f as => idp");
  }

  @Test
  public void etaTest() {
    typeCheckDef("\\func test {A : \\Type} {n : Nat} (g : Fin n -> Array A 3) : (\\new Array (Array A) n g) = {Array (Array A)} \\new Array (Array A 3) n g => idp");
  }

  @Test
  public void etaError() {
    typeCheckDef("\\func test {A : \\Type} {n : Nat} (g : Fin n -> Array A 3) : (\\new Array (Array A) n g) = {DArray} \\new Array (Array A 3) n g => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void disjointConstructorsTest() {
    typeCheckModule(
      "\\lemma test1 (p : 1 :: 2 :: nil = 1 :: 2 :: 3 :: nil) : 0 = 1\n" +
      "\\lemma test2 (p : 1 :: 2 :: 3 :: nil = 1 :: 2 :: nil) : 0 = 1\n" +
      "\\lemma test3 (a : Array Nat 3) (p : 1 :: 2 :: nil = 1 :: 2 :: 3 :: a) : 0 = 1\n" +
      "\\lemma test4 (a : Array Nat 3) (p : 1 :: 2 :: 3 :: a = 1 :: 2 :: nil) : 0 = 1\n" +
      "\\lemma test5 (p : 1 :: 2 :: 3 :: nil = 1 :: 2 :: 4 :: nil) : 0 = 1\n" +
      "\\lemma test6 (a : Array Nat 3) (p : 1 :: 2 :: 3 :: a = 1 :: 3 :: 3 :: nil) : 0 = 1\n" +
      "\\lemma test7 (a b : Array Nat 3) (p : 1 :: 2 :: 3 :: a = 1 :: 2 :: 4 :: b) : 0 = 1\n" +
      "\\lemma test8 (a : Array Nat 0) (p : 1 :: 2 :: a = 1 :: 2 :: 3 :: nil) : 0 = 1\n" +
      "\\lemma test9 (a : Array Nat 0) (p : 1 :: 2 :: 3 :: nil = 1 :: 2 :: a) : 0 = 1");
  }

  @Test
  public void disjointConstructorsError() {
    typeCheckModule(
      "\\open Array\n" +
      "\\lemma test (a : Array Nat 1) (p : 1 :: 2 :: 3 :: nil = 1 :: 2 :: a) : 0 = 1", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void disjointConstructorsError2() {
    typeCheckModule(
      "\\open Array\n" +
      "\\lemma test (a : Array Nat 1) (p : 1 :: 2 :: a = 1 :: 2 :: 3 :: nil) : 0 = 1", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void disjointConstructorsError3() {
    typeCheckModule(
      "\\open Array\n" +
      "\\lemma test (a b : Array Nat 1) (p : 1 :: 2 :: a = 1 :: 2 :: b) : 0 = 1", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void disjointConstructorsError4() {
    typeCheckModule(
      "\\open Array\n" +
      "\\lemma test (a : Array Nat 2) (p : 1 :: 2 :: a = 1 :: 2 :: 3 :: 4 :: nil) : 0 = 1", 1);
    assertThatErrorsAre(Matchers.missingClauses(1));
  }

  @Test
  public void patternMatchingTest() {
    typeCheckModule(
      "\\open Array\n" +
      "\\func f (x : Array Nat 2) : Nat\n" +
      "  | :: x (:: y nil) => x Nat.+ y\n" +
      "\\lemma test1 : f (3 :: 5 :: nil) = 8 => idp\n" +
      "\\lemma test2 : f (\\new Array Nat 2 (\\lam _ => 6)) = 12 => idp");
  }

  @Test
  public void patternMatchingTest2() {
    typeCheckModule(
      "\\open Array\n" +
      "\\func f (x : Array Nat) : Nat\n" +
      "  | nil => 0\n" +
      "  | :: x nil => x\n" +
      "  | :: x (:: y _) => x Nat.+ y\n" +
      "\\lemma test1 : f (7 :: 12 :: 22 :: nil) = 19 => idp\n" +
      "\\lemma test2 : f (\\new Array Nat 5 (\\case __ \\with { | 0 => 41 | 1 => 56 | _ => 17 })) = 97 => idp");
  }

  @Test
  public void patternMatchingTest3() {
    typeCheckModule(
      "\\open Array\n" +
      "\\func f {n : Nat} (x : Array Nat n) : Nat \\elim n, x\n" +
      "  | 0, nil => 0\n" +
      "  | suc n, :: x a => x Nat.+ f a\n" +
      "\\lemma test1 : f (7 :: 22 :: 46 :: nil) = 75 => idp\n" +
      "\\lemma test2 : f (\\new Array Nat 4 (\\lam _ => 5)) = 20 => idp");
  }

  @Test
  public void patternMatchingTest4() {
    typeCheckModule(
      "\\open Array\n" +
      "\\func f {n : Nat} (x : Array Nat (suc (suc n))) : Nat\n" +
      "  | :: x (:: y _) => x Nat.+ y\n" +
      "\\lemma test1 : f (3 :: 5 :: nil) = 8 => idp\n" +
      "\\lemma test2 : f (\\new Array Nat 2 (\\lam _ => 6)) = 12 => idp");
  }

  @Test
  public void tuplePatternTest() {
    typeCheckModule(
      "\\func test1 (x : DArray) : Fin x.len -> \\Type\n" +
      "  | (_, A, _) => A\n" +
      "\\func test2 (x : DArray) : Nat\n" +
      "  | (n, _, _) => n\n" +
      "\\func test2' {A : \\Type} (x : Array A) : Nat\n" +
      "  | (n, _) => n\n" +
      "\\func test3 (x : DArray) : \\Pi (j : Fin x.len) -> x.A j\n" +
      "  | (_, _, f) => f\n" +
      "\\func test3' {A : \\Type} (x : Array A) : Fin x.len -> A\n" +
      "  | (_, f) => f\n" +
      "\\func test6 {n : Nat} (x : DArray { | len => n }) : Fin n -> \\Type\n" +
      "  | (A, _) => A\n" +
      "\\func test7 {n : Nat} (x : DArray { | len => n }) : \\Pi (j : Fin n) -> x.A j\n" +
      "  | (_, f) => f");
  }

  @Test
  public void patternMatchingError() {
    typeCheckDef("\\func f (x : Array Nat) : Nat", 1);
    assertThatErrorsAre(Matchers.missingClauses(2));
  }

  @Test
  public void extractType() {
    typeCheckModule(
      "\\func f (x : DArray) : Fin x.len -> \\Type\n" +
      "  | nil {A} => A\n" +
      "  | :: {_} {A} _ _ => A\n" +
      "\\func test : f (1 :: nil) = (\\lam _ => Nat) => idp");
  }

  @Test
  public void extractType2() {
    typeCheckModule(
      "\\func f (n : Nat) (x : DArray {n}) : Fin x.len -> \\Type\n" +
      "  | 0, nil {A} => A\n" +
      "  | suc _, :: {A} _ _ => A\n" +
      "\\func test : f 1 (1 :: nil) = (\\lam _ => Nat) => idp");
  }

  @Test
  public void extractType3() {
    typeCheckModule(
      "\\func f (n : Nat) (x : DArray {n}) : Fin n -> \\Type\n" +
      "  | 0, nil {A} => A\n" +
      "  | suc _, :: {A} _ _ => A\n" +
      "\\func test : f 1 (1 :: nil) = (\\lam _ => Nat) => idp");
  }

  @Test
  public void goalTest() {
    Definition def = typeCheckDef("\\func test (n : Nat) : Array Nat (suc n) => {?} :: {?}", 2);
    Map<ClassField, Expression> impls = new LinkedHashMap<>();
    Expression length = new ReferenceExpression(def.getParameters());
    impls.put(Prelude.ARRAY_LENGTH, length);
    impls.put(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(Sort.SET0, new TypedSingleDependentLink(true, null, new DataCallExpression(Prelude.FIN, LevelPair.PROP, new SingletonList<>(length))), Nat()));
    assertThatErrorsAre(Matchers.goal(1), Matchers.goal(new ClassCallExpression(Prelude.DEP_ARRAY, LevelPair.SET0, impls, Sort.SET0, UniverseKind.NO_UNIVERSES)));
  }

  @Test
  public void goalTest2() {
    typeCheckDef("\\func test : Array Nat 7 => {?} :: {?} :: {?}", 3);
    Map<ClassField, Expression> impls = new LinkedHashMap<>();
    Expression length = new SmallIntegerExpression(5);
    impls.put(Prelude.ARRAY_LENGTH, length);
    impls.put(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(Sort.SET0, new TypedSingleDependentLink(true, null, new DataCallExpression(Prelude.FIN, LevelPair.PROP, new SingletonList<>(length))), Nat()));
    assertThatErrorsAre(Matchers.goal(0), Matchers.goal(0), Matchers.goal(new ClassCallExpression(Prelude.DEP_ARRAY, LevelPair.SET0, impls, Sort.SET0, UniverseKind.NO_UNIVERSES)));
  }

  @Test
  public void consTypeTest() {
    typeCheckDef("\\func test : Array (Fin 2) => 0 :: 1 :: nil");
  }

  @Test
  public void coerceTest() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func test (a : Array Nat 2) (i : Fin 2) => a i");
    assertTrue(def.getBody() instanceof AppExpression);
    Expression fun = ((AppExpression) def.getBody()).getFunction();
    assertTrue(fun instanceof FieldCallExpression && ((FieldCallExpression) fun).getDefinition() == Prelude.ARRAY_AT);
  }

  @Test
  public void inferTypeTest() {
    typeCheckModule(
      "\\func test1 : Fin 2 -> Fin 7 => DArray.at {3 :: 5 :: nil}\n" +
      "\\func test2 : Fin 2 -> Fin 7 => (3 :: 5 :: nil) DArray.!!\n" +
      "\\func test3 : Fin 2 -> Fin 7 => 3 :: 5 :: nil");
  }

  @Test
  public void coerceTest2() {
    typeCheckModule(
      "\\func test1 (P : Array Nat -> \\Type) (x : Array Nat) (p : P x) : P x => p\n" +
      "\\func test2 (P : Array Nat -> \\Type) (x : Array Nat) (p : P (x DArray.!!)) : P x => p\n" +
      "\\func test3 (P : Array Nat -> \\Type) (x : Array Nat) (p : P x.at) : P x => p\n" +
      "\\func test4 (P : Array Nat -> \\Type) (x : Array Nat) (p : P (\\lam i => x DArray.!! i)) : P x => p\n" +
      "\\func test5 (P : Array Nat -> \\Type) (x : Array Nat) (p : P (\\lam i => x.at i)) : P x => p");
  }

  @Test
  public void consEquality() {
    typeCheckDef("\\func test {A : \\Type} (x y : A) (l : Array A) => x :: l = y :: l");
  }

  @Test
  public void doublePatternMatching() {
    typeCheckDef(
      "\\func test (n : Nat) (l l' : Array Nat n) : Nat\n" +
      "  | 0, nil, nil => 0\n" +
      "  | suc n, :: a l, :: a' l' => 1");
  }

  @Test
  public void fixedLength() {
    typeCheckModule(
      "\\func f {n : Nat} (l : DArray { | len => suc n }) : l.A 0 \\elim l\n" +
      "  | :: a _ => a\n" +
      "\\func test : f (1 :: {_} {\\lam _ => Nat} 2 :: nil) = 1 => idp");
  }

  @Test
  public void fixedLength2() {
    typeCheckModule(
      "\\func f (l : DArray { | len => 0 }) : Nat \\elim l\n" +
      "  | nil => 1\n" +
      "\\func test : f (nil {\\lam _ => Nat}) = 1 => idp");
  }

  @Test
  public void fixedLength3() {
    typeCheckModule(
      "\\func f (l : Array Nat 0) : Nat \\elim l\n" +
      "  | nil => 1\n" +
      "\\func test : f nil = 1 => idp");
  }

  @Test
  public void fixedLength4() {
    typeCheckModule(
      "\\func f {n : Nat} (l : DArray { | len => n }) : Nat \\elim n, l\n" +
      "  | 0, nil => 0\n" +
      "  | suc _, :: _ _ => 1\n" +
      "\\func test : f (3 :: nil) = 1 => idp");
  }
}
