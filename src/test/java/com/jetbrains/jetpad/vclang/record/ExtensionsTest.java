package com.jetbrains.jetpad.vclang.record;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolverTestCase.resolveNamesClass;

public class ExtensionsTest {
  @Test
  public void fields() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "  \\abstract p : a = a'\n" +
        "}\n" +
        "\\function f (b : B) : \\Sigma (x : b.A) (x = b.a') => (b.a, b.p)");
  }

  @Test
  public void newTest() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "  \\abstract p : a = a'\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }");
  }

  @Test
  public void newError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a' => 0 }", 1);
  }

  @Test
  public void fieldEval() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "}\n" +
        "\\function f : \\Sigma (1 = 1) (0 = 0) =>\n" +
        "  \\let b => \\new B { A => Nat | a => 1 | a' => 0 }" +
        "  \\in  (path (\\lam _ => b.a), path (\\lam _ => b.a'))");
  }

  @Test
  public void coercion() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "}\n" +
        "\\function f (a : A) => a.a\n" +
        "\\function g : 3 = 3 => path (\\lam _ => f (\\new B { A => Nat | a' => 2 | a => 3 }))\n" +
        "\\function h (b : B { A => Nat | a => 5 }) : 5 = 5 => path (\\lam _ => b.a)");
  }

  @Test
  public void nameClashError() {
    resolveNamesClass("test",
        "\\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract x : Nat\n" +
        "}", 1);
  }

  @Test
  public void nameClashError2() {
    resolveNamesClass("test",
        "\\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\class C \\extends B {\n" +
        "  \\abstract x : Nat -> Nat\n" +
        "}", 1);
  }

  @Test
  public void nameClashError3() {
    resolveNamesClass("test",
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class D \\extends B, C {}", 1);
  }

  @Test
  public void multiple() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\abstract b : A\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\abstract c : A\n" +
        "}\n" +
        "\\class D \\extends B, C {\n" +
        "  \\abstract p : b = c\n" +
        "}\n" +
        "\\function f (d : D { A => Nat | c => 4 | b => 6 }) : 6 = 4 => d.p\n" +
        "\\function g => \\new D { A => Nat | b => 3 | c => 3 | p => path (\\lam _ => 3)}");
  }

  @Test
  public void internalInheritance() {
    typeCheckClass("\\class A { \\class B \\extends A { } }", 1);
  }
}
