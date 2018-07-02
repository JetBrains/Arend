package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.Universe;
import static com.jetbrains.jetpad.vclang.typechecking.Matchers.error;
import static com.jetbrains.jetpad.vclang.typechecking.Matchers.hasErrors;
import static com.jetbrains.jetpad.vclang.typechecking.Matchers.typeMismatchError;
import static org.junit.Assert.assertEquals;

public class RecordsTest extends TypeCheckingTestCase {
  @Test
  public void nonDependentImplement() {
    typeCheckModule(
        "\\class Point { | x : Nat | y : Nat }\n" +
        "\\func p => \\new Point { x => 0 | y => 0 }");
  }

  @Test
  public void nonDependentImplementError() {
    typeCheckModule(
        "\\class Point { | x : Nat | y : Nat }\n" +
        "\\func C => Point { x => \\lam (t : Nat) => t }", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void dependentImplement() {
    typeCheckModule(
        "\\class Point { | X : \\Set | y : X -> Nat }\n" +
        "\\func p => \\new Point { X => Nat | y => \\lam n => n }");
  }

  @Test
  public void parentCallTypecheckingTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | c : Nat -> Nat -> Nat\n" +
        "  | f : Nat -> Nat\n" +
        "}\n" +
        "\\func B => A {\n" +
        "  f => \\lam n => c n n\n" +
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
        "\\func test (p : diagonal 0) : p.x = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void notImplementedTest() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : Nat\n" +
        "}\n" +
        "\\func diagonal => Point { y => 0 }");
  }

  @Test
  public void notImplementedTestError() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : x = x -> Nat\n" +
        "}\n" +
        "\\func diagonal => Point { y => \\lam _ => 0 }", 1);
  }

  @Test
  public void newError() {
    typeCheckModule(
      "\\class Point { | x : Nat | y : Nat }\n" +
      "\\func C => \\new Point { x => 0 }", 1);
  }

  @Test
  public void newFunctionError() {
    typeCheckModule(
        "\\class Point {\n" +
        "  | x : Nat\n" +
        "  | y : Nat\n" +
        "}\n" +
        "\\func diagonal => Point { x => 0 }\n" +
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
        "\\func diagonal1 => Point {\n" +
        "  | x => 0\n" +
        "  | y => 0\n" +
        "}\n" +
        "\\func test : \\new diagonal1 {} = \\new diagonal 0 => path (\\lam _ => \\new Point { x => 0 | y => 0 })");
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
    ChildGroup result = typeCheckModule(
        "\\class Point { | x : Nat | y : Nat }\n" +
        "\\func C => Point { x => 0 }");
    assertEquals(Sort.SET0, ((ClassDefinition) getDefinition(result, "Point")).getSort());
    assertEquals(Universe(Sort.SET0), getDefinition(result, "C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordUniverseTest2() {
    ChildGroup result = typeCheckModule(
        "\\class Point { | x : Nat | y : Nat }\n" +
        "\\func C => Point { x => 0 | y => 1 }");
    assertEquals(Sort.SET0, ((ClassDefinition) getDefinition(result, "Point")).getSort());
    assertEquals(Universe(Sort.PROP), getDefinition(result, "C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordUniverseTest3() {
    ChildGroup result = typeCheckModule(
        "\\class Point { | x : \\Type3 | y : \\Type1 }\n" +
        "\\func C => Point { x => Nat }");
    assertEquals(new Sort(new Level(4), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition(result, "Point")).getSort());
    assertEquals(Universe(new Sort(new Level(2), new Level(LevelVariable.HVAR, 1))), getDefinition(result, "C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordUniverseTest4() {
    ChildGroup result = typeCheckModule(
        "\\class Point { | x : \\Type3 | y : \\oo-Type1 }\n" +
        "\\func C => Point { x => Nat }");
    assertEquals(new Sort(new Level(4), Level.INFINITY), ((ClassDefinition) getDefinition(result, "Point")).getSort());
    assertEquals(Universe(new Sort(new Level(2), Level.INFINITY)), getDefinition(result, "C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordUniverseTest5() {
    ChildGroup result = typeCheckModule(
        "\\class Point { | x : \\Type3 | y : \\Type1 }\n" +
        "\\func C => Point { x => \\Type2 }");
    assertEquals(new Sort(new Level(4), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition(result, "Point")).getSort());
    assertEquals(Universe(new Sort(new Level(2), new Level(LevelVariable.HVAR, 2))), getDefinition(result, "C").getTypeWithParams(new ArrayList<>(), Sort.STD));
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
    assertThatErrorsAre(error());
  }

  @Test
  public void mutuallyRecursiveClassError() {
    typeCheckModule(
      "\\class A { | a : B }\n" +
      "\\class B { | b : A }", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void recursiveFieldsError() {
    ChildGroup module = typeCheckModule(
      "\\class X {\n" +
      "  | A : (B (\\lam _ => 0) -> Nat) -> \\Prop\n" +
      "  | B : (A (\\lam _ => 0) -> Nat) -> \\Prop\n" +
      "}", 2);
    assertThatErrorsAre(error(), hasErrors(getDefinition(module, "X.A").getReferable()));
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
      "\\func const (c : C) => \\new C { A => c.A -> c.A | a => \\lam _ => c.a }\n" +
      "\\func const' (c : C) : C { A => c.A -> c.A } => \\new C { | A => c.A -> c.A | a => \\lam _ => c.a }\n" +
      "\\func test' (f : (C -> C) -> Nat) => f const'\n" +
      "\\func test (f : (\\Pi (c : C) -> C { A => c.A -> c.A }) -> Nat) => f const");
  }
}
