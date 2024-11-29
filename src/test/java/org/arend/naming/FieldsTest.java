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
    resolveNamesModule("""
      \\class A { | f : Nat }
      \\class B { | g : A }
      \\func test (b : B) => b.g.f
      """);
  }

  @Test
  public void testFieldParameter() {
    resolveNamesModule(
      "\\class A (g : Nat) { | f : Nat }\n" +
      "\\func test (a : A) => a.g");
  }

  @Test
  public void testFunction() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func func : A
      \\func test => func.f
      """);
  }

  @Test
  public void testInstance() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\instance inst : A
      \\func test => inst.g
      """);
  }

  @Test
  public void testExtended() {
    resolveNamesModule("""
      \\class A { | f : Nat }
      \\class B \\extends A { | g : Nat }
      \\func test (b : B) => b.f
      """);
  }

  @Test
  public void testFunctionWithParameters() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func func (x : Nat) : A
      \\func test => func.f
      """);
  }

  @Test
  public void testFunctionWithImplicitParameters() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func func {x y : Nat} {z : Nat -> Nat} : A
      \\func test => func.f
      """);
  }

  @Test
  public void testComplex() {
    resolveNamesModule("""
      \\class A (f : Nat) { | g : Nat }
      \\class B \\extends A { | h : Nat }
      \\class C { | a : A | b : B }
      \\func test1 (c : C) => c.b.f
      \\func test2 (c : C) => c.b.g
      \\func test3 (c : C) => c.b.h
      """);
  }

  @Test
  public void testFunctionReference() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func B => A
      \\func test (a : B) => a.f
      """);
  }

  @Test
  public void testIteratedFunctionReference() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func C => B
      \\func B => A
      \\func test (a : C) => a.g
      """);
  }

  @Test
  public void testRecursiveFunctionReference() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func C => B
      \\func B => C
      \\func test (a : C) => a.f
      """);
  }

  @Test
  public void testFunctionWithParametersReference() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func B (x : Nat) => A
      \\func test (a : B 0) => a.f
      """);
  }

  @Test
  public void testFunctionWithNoParametersReference() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func B (x : Nat) => A
      \\func test (a : B) => a.f
      """);
  }

  @Test
  public void testInfixFunctionWithParametersReference() {
    resolveNamesModule("""
      \\class A { | f : Nat | g : Nat }
      \\func \\infixr 3 + (x y : Nat) => A
      \\func test (a : 0 + 1) => a.g
      """);
  }

  @Test
  public void testTwoFunctionsWithParameters() {
    resolveNamesModule("""
      \\record R (q : Nat)
      \\func S (n : Nat) => R
      \\func T (n : Nat) => S n
      \\func h (r : T 0) => r.q
      """);
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
    resolveNamesModule("""
      \\class A (f : Nat)
      \\data D | con A
      \\func foo (d : D) : Nat | con (a : A) => a.f
      """);
  }

  @Test
  public void metaTest() {
    resolveNamesModule("""
      \\record R (x : Nat)
      \\func test (m : M) => m.x
      \\meta M => R
      """);
  }
}
