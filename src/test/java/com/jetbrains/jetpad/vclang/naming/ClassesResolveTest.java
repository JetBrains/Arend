package com.jetbrains.jetpad.vclang.naming;

import org.junit.Test;

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
      "\\function f => \\new D { A => \\Prop }");
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
  public void clashingNamesSuper() {
    resolveNamesModule(
      "\\class C1 { | A : \\Set }\n" +
      "\\class C2 { | A : \\Set }\n" +
      "\\class D \\extends C1, C2 { | a : C1.A | b : C2.A }");
  }

  @Test
  public void clashingNamesSuperError() {
    resolveNamesModule(
      "\\class C1 { | A : \\Set }\n" +
      "\\class C2 { | A : \\Set }\n" +
      "\\class D \\extends C1, C2 { | a : A }", 1);
  }

  @Test
  public void clashingNamesSuperImplement() {
    resolveNamesModule(
      "\\class C1 { | A : \\Set }\n" +
      "\\class C2 { | A : \\Set }\n" +
      "\\class D \\extends C1, C2 { | C1.A => \\Prop | C2.A => \\Prop -> \\Prop }");
  }

  @Test
  public void clashingNamesSuperImplementError() {
    resolveNamesModule(
      "\\class C1 { | A : \\Set }\n" +
      "\\class C2 { | A : \\Set }\n" +
      "\\class D \\extends C1, C2 { | A => \\Prop }", 1);
  }
}
