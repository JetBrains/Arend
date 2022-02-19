package org.arend.classes;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.DataCallExpression;
import org.arend.core.expr.NewExpression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.arend.ExpressionFactory.Universe;
import static org.junit.Assert.assertEquals;

public class RecordsTest extends TypeCheckingTestCase {
  @Test
  public void nonDependentImplement() {
    typeCheckModule(
        "\\class Point { | x : Nat | y : Nat }\n" +
        "\\func p => \\new Point { | x => 0 | y => 0 }");
  }

  @Test
  public void nonDependentImplementError() {
    typeCheckModule(
        "\\class Point { | x : Nat | y : Nat }\n" +
        "\\func C => Point { | x => \\lam (t : Nat) => t }", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void dependentImplement() {
    typeCheckModule(
        "\\class Point { | X : \\Set | y : X -> Nat }\n" +
        "\\func p => \\new Point { | X => Nat | y => \\lam n => n }");
  }

  @Test
  public void parentCallTypecheckingTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | c : Nat -> Nat -> Nat\n" +
        "  | f : Nat -> Nat\n" +
        "}\n" +
        "\\func B => A {\n" +
        "  | f => \\lam n => c n n\n" +
        "}", 1);
  }

  @Test
  public void duplicateNameTestError() {
    typeCheckModule(
        "\\class A {\n" +
        "  | f : Nat\n" +
        "}\n" +
        "\\func B => A {\n" +
        "  | f => 0\n" +
        "  | f => 1\n" +
        "}", 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(true, Collections.singletonList(get("f"))));
  }

  @Test
  public void overriddenFieldAccTest() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : Nat\n" +
        "}\n" +
        "\\func diagonal => \\lam (d : Nat) => Point {\n" +
        "  | x => d\n" +
        "  | y => d\n" +
        "}\n" +
        "\\func test (p : diagonal 0) : p.x = 0 => idp");
  }

  @Test
  public void notImplementedTest() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : Nat\n" +
        "}\n" +
        "\\func diagonal => Point { | y => 0 }");
  }

  @Test
  public void notImplementedTestError() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : x = x -> Nat\n" +
        "}\n" +
        "\\func diagonal => Point { | y => \\lam _ => 0 }");
  }

  @Test
  public void newError() {
    typeCheckModule(
      "\\class Point { | x : Nat | y : Nat }\n" +
      "\\func C => \\new Point { | x => 0 }", 1);
  }

  @Test
  public void newFunctionError() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : Nat\n" +
        "}\n" +
        "\\func diagonal => Point { | x => 0 }\n" +
        "\\func test => \\new diagonal", 1);
  }

  @Test
  public void newTest() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : Nat\n" +
        "}\n" +
        "\\func diagonal => \\lam (d : Nat) => Point {\n" +
        "  | x => d\n" +
        "  | y => d\n" +
        "}\n" +
        "\\func diagonal1 => \\new Point {\n" +
        "  | x => 0\n" +
        "  | y => 0\n" +
        "}\n" +
        "\\func test : diagonal1 = \\new diagonal 0 => path (\\lam _ => \\new Point { | x => 0 | y => 0 })");
  }

  @Test
  public void mutualRecursionTypecheckingError() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : Nat\n" +
        "}\n" +
        "\\func test => Point {\n" +
        "  | x => y\n" +
        "  | y => x\n" +
        "}", 2);
  }

  @Test
  public void recordUniverseTest() {
    typeCheckModule(
        "\\class Point { | x : Nat | y : Nat }\n" +
        "\\func C => Point { | x => 0 }");
    Assert.assertEquals(Sort.SET0, ((ClassDefinition) getDefinition("Point")).getSort());
    assertEquals(Universe(Sort.SET0), getDefinition("C").getTypeWithParams(new ArrayList<>(), LevelPair.STD));
  }

  @Test
  public void recordUniverseTest2() {
    typeCheckModule(
        "\\class Point { | x : Nat | y : Nat }\n" +
        "\\func C => Point { | x => 0 | y => 1 }");
    Assert.assertEquals(Sort.SET0, ((ClassDefinition) getDefinition("Point")).getSort());
    assertEquals(Universe(Sort.PROP), getDefinition("C").getTypeWithParams(new ArrayList<>(), LevelPair.STD));
  }

  @Test
  public void recordUniverseTest3() {
    typeCheckModule(
        "\\class Point { | x : \\Type3 | y : \\Type1 }\n" +
        "\\func C => Point { | x => Nat }");
    Assert.assertEquals(new Sort(new Level(4), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition("Point")).getSort());
    assertEquals(Universe(new Sort(new Level(2), new Level(LevelVariable.HVAR, 1, 1))), getDefinition("C").getTypeWithParams(new ArrayList<>(), LevelPair.STD));
  }

  @Test
  public void recordUniverseTest4() {
    typeCheckModule(
        "\\class Point { | x : \\Type3 | y : \\oo-Type1 }\n" +
        "\\func C => Point { | x => Nat }");
    Assert.assertEquals(new Sort(new Level(4), Level.INFINITY), ((ClassDefinition) getDefinition("Point")).getSort());
    assertEquals(Universe(new Sort(new Level(2), Level.INFINITY)), getDefinition("C").getTypeWithParams(new ArrayList<>(), LevelPair.STD));
  }

  @Test
  public void recordUniverseTest5() {
    typeCheckModule(
        "\\class Point { | x : \\Type3 | y : \\Type1 }\n" +
        "\\func C => Point { | x => \\Type2 }");
    Assert.assertEquals(new Sort(new Level(4), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition("Point")).getSort());
    assertEquals(Universe(new Sort(new Level(2), new Level(LevelVariable.HVAR, 2))), getDefinition("C").getTypeWithParams(new ArrayList<>(), LevelPair.STD));
  }

  @Test
  public void fieldCallInClass() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\class B {\n" +
      "  | a : A\n" +
      "  | y : a.x = a.x\n" +
      "}");
  }

  @Test
  public void fieldCallInClass2() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\class B {\n" +
      "  | a : A\n" +
      "  | y : a.x = a.x\n" +
      "  | z : y = y\n" +
      "}");
  }

  @Test
  public void fieldCallInClass3() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\class B {\n" +
      "  | a : A\n" +
      "  | y : path (\\lam _ => a.x) = path (\\lam _ => a.x)\n" +
      "}");
  }

  @Test
  public void fieldCallWithArg0() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\class B {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func y (b : B) => b.a.x");
  }

  @Test
  public void fieldCallWithArg1() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\class B {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func y (b : Nat -> B) => x {a {b 0}}");
  }

  @Test
  public void fieldCallWithArg2() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\class B {\n" +
      "  | a : Nat -> A\n" +
      "}\n" +
      "\\func y (b : B) => x {b.a 1}");
  }

  @Test
  public void fieldCallWithArg3() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\class B {\n" +
      "  | a : Nat -> A\n" +
      "}\n" +
      "\\func y (b : Nat -> B) => x {a {b 0} 1}");
  }

  @Test
  public void recursiveClassError() {
    typeCheckModule("\\class A { | a : A }", 1);
    assertThatErrorsAre(Matchers.error());
  }

  @Test
  public void mutuallyRecursiveClassError() {
    typeCheckModule(
      "\\class A { | a : B }\n" +
      "\\class B { | b : A }", 1);
    assertThatErrorsAre(Matchers.error());
  }

  @Test
  public void recursiveFieldsError() {
    typeCheckModule(
      "\\class X {\n" +
      "  | A : (B (\\lam _ => 0) -> Nat) -> \\Prop\n" +
      "  | B : (A (\\lam _ => 0) -> Nat) -> \\Prop\n" +
      "}", 1);
    assertThatErrorsAre(Matchers.error());
  }

  @Test
  public void infixTest() {
    typeCheckModule(
      "\\class A { | \\infix 5 + : Nat -> Nat -> Nat }\n" +
      "\\func f (a : A) => 0 a.+ 0");
  }

  @Test
  public void higherFunctionsTest() {
    typeCheckModule(
      "\\class C { | A : \\Set | a : A }\n" +
      "\\func const (c : C) => \\new C { | A => c.A -> c.A | a => \\lam _ => c.a }\n" +
      "\\func const' (c : C) : C { | A => c.A -> c.A } => \\new C { | A => c.A -> c.A | a => \\lam _ => c.a }\n" +
      "\\func test' (f : (C -> C) -> Nat) => f const'\n" +
      "\\func test (f : (\\Pi (c : C) -> C { | A => c.A -> c.A }) -> Nat) => f const");
  }

  @Test
  public void missingFieldsFromExpectedType() {
    typeCheckModule(
      "\\record X (A : \\Type) (B : A -> A)\n" +
      "\\func test {A : \\Type} (x : X A) : X A (x.B) => \\new X {}");
  }

  @Test
  public void missingFieldsFromExpectedType2() {
    typeCheckModule(
      "\\record X (A : \\Type) (B : A -> A)\n" +
      "\\func test {A : \\Type} (x : X A) : X A (x.B) => \\new X");
  }

  @Test
  public void swapTest() {
    typeCheckModule(
      "\\record Pair (A B : \\Type)\n" +
      "  | fst : A\n" +
      "  | snd : B\n" +
      "\\func swap {A B : \\Type} (p : Pair A B) : Pair B A \\cowith\n" +
      "  | fst => p.snd\n" +
      "  | snd => p.fst\n" +
      "\\func idpe {A : \\Type} (a : A) : a = a => idp\n" +
      "\\func swap-inv1 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idp\n" +
      "\\func swap-inv2 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idp {_} {_}\n" +
      "\\func swap-inv3 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idp {_} {p}\n" +
      "\\func swap-inv4 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idp {_} {\\new Pair A B p.fst p.snd}\n" +
      "\\func swap-inv5 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idpe _\n" +
      "\\func swap-inv6 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idpe p\n" +
      "\\func swap-inv7 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idpe (\\new Pair A B p.fst p.snd)");
  }

  @Test
  public void newSubClass() {
    typeCheckModule(
      "\\record A (n : Nat)\n" +
      "\\record B \\extends A | n => 0\n" +
      "\\data D (a : A) | ddd\n" +
      "\\func b : D (\\new B) => ddd");
    assertEquals(getDefinition("B"), ((NewExpression) ((DataCallExpression) ((FunctionDefinition) getDefinition("b")).getResultType()).getDefCallArguments().get(0)).getClassCall().getDefinition());
  }

  @Test
  public void resolveIncorrect() {
    typeCheckModule(
      "\\class C { | A : \\Set }\n" +
      "\\class D { | B : \\Set }\n" +
      "\\func f => \\new D { | A => \\Prop }", 1);
  }

  @Test
  public void resolveIncorrect2() {
    typeCheckModule(
      "\\class C { | A : \\Set }\n" +
      "\\class D { | B : \\Set }\n" +
      "\\class E \\extends D\n" +
      "\\func f => \\new E { | C => \\new C \\Prop }", 1);
  }

  @Test
  public void resolveNotSuperImplement() {
    typeCheckModule(
      "\\class A { | x : Nat }\n" +
      "\\class B { | y : Nat }\n" +
      "\\class C \\extends B { | A => \\new A { | x => 0 } }", 1);
  }
}
