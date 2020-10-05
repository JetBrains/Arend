package org.arend.naming;

import org.junit.Test;

public class FieldsTest extends NameResolverTestCase {
  @Test
  public void testLocalVariable() {
    resolveNamesModule(
      "\\class C { | f : Nat | g : Nat }\n" +
      "\\func test (c : C) => c.f");
  }

  @Test
  public void testIterated() {
    resolveNamesModule(
      "\\class A { | f : Nat }\n" +
      "\\class B { | g : A }\n" +
      "\\func test (b : B) => b.g.f");
  }

  @Test
  public void testFieldParameter() {
    resolveNamesModule(
      "\\class A (g : Nat) { | f : Nat }\n" +
      "\\func test (a : A) => a.g");
  }

  @Test
  public void testFunction() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func func : A\n" +
      "\\func test => func.f");
  }

  @Test
  public void testInstance() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\instance inst : A\n" +
      "\\func test => inst.g");
  }

  @Test
  public void testExtended() {
    resolveNamesModule(
      "\\class A { | f : Nat }\n" +
      "\\class B \\extends A { | g : Nat }\n" +
      "\\func test (b : B) => b.f");
  }

  @Test
  public void testFunctionWithParameters() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func func (x : Nat) : A\n" +
      "\\func test => func.f", 1);
  }

  @Test
  public void testFunctionWithImplicitParameters() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func func {x y : Nat} {z : Nat -> Nat} : A\n" +
      "\\func test => func.f");
  }

  @Test
  public void testComplex() {
    resolveNamesModule(
      "\\class A (f : Nat) { | g : Nat }\n" +
      "\\class B \\extends A { | h : Nat }\n" +
      "\\class C { | a : A | b : B }\n" +
      "\\func test1 (c : C) => c.b.f\n" +
      "\\func test2 (c : C) => c.b.g\n" +
      "\\func test3 (c : C) => c.b.h");
  }

  @Test
  public void testFunctionReference() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func B => A\n" +
      "\\func test (a : B) => a.f");
  }

  @Test
  public void testIteratedFunctionReference() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func C => B\n" +
      "\\func B => A\n" +
      "\\func test (a : C) => a.g");
  }

  @Test
  public void testRecursiveFunctionReference() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func C => B\n" +
      "\\func B => C\n" +
      "\\func test (a : C) => a.f", 1);
  }

  @Test
  public void testFunctionWithParametersReference() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func B (x : Nat) => A\n" +
      "\\func test (a : B 0) => a.f");
  }

  @Test
  public void testFunctionWithNoParametersReference() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func B (x : Nat) => A\n" +
      "\\func test (a : B) => a.f", 1);
  }

  @Test
  public void testInfixFunctionWithParametersReference() {
    resolveNamesModule(
      "\\class A { | f : Nat | g : Nat }\n" +
      "\\func \\infixr 3 + (x y : Nat) => A\n" +
      "\\func test (a : 0 + 1) => a.g");
  }

  @Test
  public void testTwoFunctionsWithParameters() {
    resolveNamesModule(
      "\\record R (q : Nat)\n" +
      "\\func S (n : Nat) => R\n" +
      "\\func T (n : Nat) => S n\n" +
      "\\func h (r : T 0) => r.q");
  }

  @Test
  public void letTest() {
    resolveNamesModule(
      "\\class A (f : Nat)\n" +
      "\\func foo (b : A) => \\let a : A => b \\in a.f");
  }

  @Test
  public void letNewTest() {
    resolveNamesModule(
      "\\class A (f : Nat)\n" +
      "\\func foo => \\let a => \\new A 0 \\in a.f");
  }

  @Test
  public void patternTest() {
    resolveNamesModule(
      "\\class A (f : Nat)\n" +
      "\\data D | con A\n" +
      "\\func foo (d : D) : Nat | con (a : A) => a.f");
  }
}
