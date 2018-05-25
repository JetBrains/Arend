package com.jetbrains.jetpad.vclang.naming;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.warning;

public class ClassesResolveTest extends NameResolverTestCase {
  @Test
  public void resolveParameter() {
    resolveNamesDef("\\class C (A : \\Set) { | a : A }");
  }

  @Test
  public void unknownExtTestError() {
    resolveNamesModule(
      "\\class Point { | x : Nat | y : Nat }\n" +
      "\\func C => Point { x => 0 | z => 0 | y => 0 }", 1);
  }

  @Test
  public void parentCallTest() {
    resolveNamesModule(
      "\\class M {\n" +
      "  \\class A {\n" +
      "    | c : Nat -> Nat -> Nat\n" +
      "    | f : Nat -> Nat\n" +
      "  }\n" +
      "}\n" +
      "\\func B => M.A {\n" +
      "  f => \\lam n => c n n\n" +
      "}", 1);
  }

  @Test
  public void mutualRecursionTestError() {
    resolveNamesModule(
      "\\class M {\n" +
        "  \\class Point {\n" +
        "    | x : Nat\n" +
        "    | y : Nat\n" +
        "  }\n" +
        "}\n" +
        "\\func test => M.Point {\n" +
        "  | x => y\n" +
        "  | y => x\n" +
        "}", 2);
  }

  @Test
  public void splitClassTestError() {
    resolveNamesModule(
      "\\class A \\where {\n" +
      "  \\func x => 0\n" +
      "}\n" +
      "\\class A \\where {\n" +
      "  \\func y => 0\n" +
      "}", 1);
  }

  @Test
  public void resolveIncorrect() {
    resolveNamesModule(
      "\\class C { | A : \\Set }\n" +
      "\\class D { | B : \\Set }\n" +
      "\\func f => \\new D { A => \\Prop }", 1);
  }

  @Test
  public void resolveParameterSuper() {
    resolveNamesModule(
      "\\class C (A : \\Set)\n" +
      "\\class D \\extends C { | a : A }");
  }

  @Test
  public void resolveFieldSuper() {
    resolveNamesModule(
      "\\class C { | A : \\Set }\n" +
      "\\class D \\extends C { | a : A }");
  }

  @Test
  public void resolveFieldMultipleSuper() {
    resolveNamesModule(
      "\\class C1 { | A : \\Set }\n" +
      "\\class C2 { | B : \\Set }\n" +
      "\\class D \\extends C1, C2 { | a : A | b : B }");
  }

  @Test
  public void resolveSuperImplement() {
    resolveNamesModule(
      "\\class A { | x : Nat }\n" +
      "\\class B \\extends A { | y : Nat }\n" +
      "\\class C \\extends B { | A => \\new A { x => 0 } }");
  }

  @Test
  public void resolveNotSuperImplement() {
    resolveNamesModule(
      "\\class A { | x : Nat }\n" +
      "\\class B { | y : Nat }\n" +
      "\\class C \\extends B { | A => \\new A { x => 0 } }", 1);
  }

  @Test
  public void clashingNamesSuper() {
    resolveNamesModule(
      "\\class X \\where { \\class C1 { | A : \\Set } }\n" +
      "\\class Y \\where { \\class C2 { | A : \\Set } }\n" +
      "\\class D \\extends X.C1, Y.C2 { | a : X.C1.A | b : Y.C2.A }");
  }

  @Test
  public void clashingNamesSuper2() {
    resolveNamesModule(
      "\\class X \\where { \\class C1 { | A : \\Set } }\n" +
      "\\class Y \\where { \\class C2 { | A : \\Set } }\n" +
      "\\class D \\extends X.C1, Y.C2 { | a : A }");
  }

  @Test
  public void clashingNamesSuperImplement() {
    resolveNamesModule(
      "\\class X \\where { \\class C1 { | A : \\Set } }\n" +
      "\\class Y \\where { \\class C2 { | A : \\Set } }\n" +
      "\\class D \\extends X.C1, Y.C2 { | C1.A => \\Prop | C2.A => \\Prop -> \\Prop }");
  }

  @Test
  public void clashingNamesSuperImplement2() {
    resolveNamesModule(
      "\\class X \\where { \\class C1 { | A : \\Set } }\n" +
      "\\class Y \\where { \\class C2 { | A : \\Set } }\n" +
      "\\class D \\extends X.C1, Y.C2 { | A => \\Prop }");
  }

  @Test
  public void clashingNamesSuperCurrent() {
    resolveNamesModule(
      "\\class X \\where { \\class C1 { | A : \\Set } \\class C2 \\extends C1 }\n" +
      "\\class D \\extends X.C2 { | A : \\Prop }", 1);
    assertThatErrorsAre(warning());
  }
}
