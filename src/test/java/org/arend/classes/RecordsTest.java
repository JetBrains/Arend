package org.arend.classes;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.DataCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.NewExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.ops.CMP;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.arend.ExpressionFactory.Universe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
    typeCheckModule("""
      \\class A {
        | c : Nat -> Nat -> Nat
        | f : Nat -> Nat
      }
      \\func B => A {
        | f => \\lam n => c n n
      }
      """, 1);
  }

  @Test
  public void duplicateNameTestError() {
    typeCheckModule("""
      \\class A {
        | f : Nat
      }
      \\func B => A {
        | f => 0
        | f => 1
      }
      """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(true, Collections.singletonList(get("f"))));
  }

  @Test
  public void overriddenFieldAccTest() {
    typeCheckModule("""
      \\class Point {
        | x : Nat
        | y : Nat
      }
      \\func diagonal => \\lam (d : Nat) => Point {
        | x => d
        | y => d
      }
      \\func test (p : diagonal 0) : p.x = 0 => idp
      """);
  }

  @Test
  public void notImplementedTest() {
    typeCheckModule("""
      \\class Point {
        | x : Nat
        | y : Nat
      }
      \\func diagonal => Point { | y => 0 }
      """);
  }

  @Test
  public void notImplementedTestError() {
    typeCheckModule("""
      \\class Point {
        | x : Nat
        | y : x = x -> Nat
      }
      \\func diagonal => Point { | y => \\lam _ => 0 }
      """);
  }

  @Test
  public void newError() {
    typeCheckModule(
      "\\class Point { | x : Nat | y : Nat }\n" +
      "\\func C => \\new Point { | x => 0 }", 1);
  }

  @Test
  public void newFunctionError() {
    typeCheckModule("""
      \\class Point {
        | x : Nat
        | y : Nat
      }
      \\func diagonal => Point { | x => 0 }
      \\func test => \\new diagonal
      """, 1);
  }

  @Test
  public void newTest() {
    typeCheckModule("""
      \\class Point {
        | x : Nat
        | y : Nat
      }
      \\func diagonal => \\lam (d : Nat) => Point {
        | x => d
        | y => d
      }
      \\func diagonal1 => \\new Point {
        | x => 0
        | y => 0
      }
      \\func test : diagonal1 = \\new diagonal 0 => path (\\lam _ => \\new Point { | x => 0 | y => 0 })
      """);
  }

  @Test
  public void mutualRecursionTypecheckingError() {
    typeCheckModule("""
      \\class Point {
        | x : Nat
        | y : Nat
      }
      \\func test => Point {
        | x => y
        | y => x
      }
      """, 2);
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
    typeCheckModule("""
      \\class A {
        | x : Nat
      }
      \\class B {
        | a : A
        | y : a.x = a.x
      }
      """);
  }

  @Test
  public void fieldCallInClass2() {
    typeCheckModule("""
      \\class A {
        | x : Nat
      }
      \\class B {
        | a : A
        | y : a.x = a.x
        | z : y = y
      }
      """);
  }

  @Test
  public void fieldCallInClass3() {
    typeCheckModule("""
      \\class A {
        | x : Nat
      }
      \\class B {
        | a : A
        | y : path (\\lam _ => a.x) = path (\\lam _ => a.x)
      }
      """);
  }

  @Test
  public void fieldCallWithArg0() {
    typeCheckModule("""
      \\class A {
        | x : Nat
      }
      \\class B {
        | a : A
      }
      \\func y (b : B) => b.a.x
      """);
  }

  @Test
  public void fieldCallWithArg1() {
    typeCheckModule("""
      \\class A {
        | x : Nat
      }
      \\class B {
        | a : A
      }
      \\func y (b : Nat -> B) => x {a {b 0}}
      """);
  }

  @Test
  public void fieldCallWithArg2() {
    typeCheckModule("""
      \\class A {
        | x : Nat
      }
      \\class B {
        | a : Nat -> A
      }
      \\func y (b : B) => x {b.a 1}
      """);
  }

  @Test
  public void fieldCallWithArg3() {
    typeCheckModule("""
      \\class A {
        | x : Nat
      }
      \\class B {
        | a : Nat -> A
      }
      \\func y (b : Nat -> B) => x {a {b 0} 1}
      """);
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
    typeCheckModule("""
      \\class X {
        | A : (B (\\lam _ => 0) -> Nat) -> \\Prop
        | B : (A (\\lam _ => 0) -> Nat) -> \\Prop
      }
      """, 1);
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
    typeCheckModule("""
      \\class C { | A : \\Set | a : A }
      \\func const (c : C) => \\new C { | A => c.A -> c.A | a => \\lam _ => c.a }
      \\func const' (c : C) : C { | A => c.A -> c.A } => \\new C { | A => c.A -> c.A | a => \\lam _ => c.a }
      \\func test' (f : (C -> C) -> Nat) => f const'
      \\func test (f : (\\Pi (c : C) -> C { | A => c.A -> c.A }) -> Nat) => f const
      """);
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
    typeCheckModule("""
      \\record Pair (A B : \\Type)
        | fst : A
        | snd : B
      \\func swap {A B : \\Type} (p : Pair A B) : Pair B A \\cowith
        | fst => p.snd
        | snd => p.fst
      \\func idpe {A : \\Type} (a : A) : a = a => idp
      \\func swap-inv1 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idp
      \\func swap-inv2 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idp {_} {_}
      \\func swap-inv3 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idp {_} {p}
      \\func swap-inv4 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idp {_} {\\new Pair A B p.fst p.snd}
      \\func swap-inv5 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idpe _
      \\func swap-inv6 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idpe p
      \\func swap-inv7 {A B : \\Type} (p : Pair A B) : swap (swap p) = p => idpe (\\new Pair A B p.fst p.snd)
      """);
  }

  @Test
  public void newSubClass() {
    typeCheckModule("""
      \\record A (n : Nat)
      \\record B \\extends A | n => 0
      \\data D (a : A) | ddd
      \\func b : D (\\new B) => ddd
      """);
    assertEquals(getDefinition("B"), ((NewExpression) ((DataCallExpression) ((FunctionDefinition) getDefinition("b")).getResultType()).getDefCallArguments().get(0)).getClassCall().getDefinition());
  }

  @Test
  public void resolveIncorrect() {
    typeCheckModule("""
      \\class C { | A : \\Set }
      \\class D { | B : \\Set }
      \\func f => \\new D { | A => \\Prop }
      """, 2);
  }

  @Test
  public void resolveIncorrect2() {
    typeCheckModule("""
      \\class C { | A : \\Set }
      \\class D { | B : \\Set }
      \\class E \\extends D
      \\func f => \\new E { | C => \\new C \\Prop }
      """, 2);
  }

  @Test
  public void resolveNotSuperImplement() {
    typeCheckModule("""
      \\class A { | x : Nat }
      \\class B { | y : Nat }
      \\class C \\extends B { | A => \\new A { | x => 0 } }
      """, 1);
  }

  @Test
  public void comparisonTest() {
    typeCheckModule("""
      \\record R (n m : Nat)
      \\func test1 => R
      \\func test2 (n : Nat) => R n
      """);
    Expression expr1 = (Expression) ((FunctionDefinition) getDefinition("test1")).getBody();
    Expression expr2 = (Expression) ((FunctionDefinition) getDefinition("test2")).getBody();
    assertFalse(CompareVisitor.compare(DummyEquations.getInstance(), CMP.EQ, expr1, expr2, Type.OMEGA, null));
    assertFalse(CompareVisitor.compare(DummyEquations.getInstance(), CMP.EQ, expr2, expr1, Type.OMEGA, null));
  }
}
