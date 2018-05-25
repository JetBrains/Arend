package com.jetbrains.jetpad.vclang.naming;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.warning;

public class ClassesResolveTest extends NameResolverTestCase {
  @Test
  public void resolveParameter() {
    resolveNamesDef("\\class C (A : \\Set) { | a : A }");
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
