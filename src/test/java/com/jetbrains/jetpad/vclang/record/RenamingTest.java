package com.jetbrains.jetpad.vclang.record;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;

public class RenamingTest {
  @Test
  public void renaming() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A \\renaming A \\to A' {\n" +
        "  \\abstract a' : A'\n" +
        "  \\abstract p : a = a'\n" +
        "}\n" +
        "\\function f (b : B) : \\Sigma (x : b.A') (x = b.a') => (b.a, b.p)");
  }

  @Test
  public void renaming2() {
    typeCheckClass(
        "\\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class C \\extends B \\renaming a \\to a' {\n" +
        "  \\abstract a'' : A\n" +
        "  \\abstract p : a' = a''\n" +
        "}\n" +
        "\\function f (c : C) : \\Sigma (x : c.A) (x = c.a'') => (c.a', c.p)");
  }

  @Test
  public void renaming3() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming A \\to A', a \\to a' {\n" +
        "  \\abstract a'' : A'\n" +
        "  \\abstract p : a' = a''\n" +
        "}\n" +
        "\\function f (b : B) (x : b.A) => b.a\n" +
        "\\function g (c : C) : (f c c.a'' = c.a'') => c.p");
  }

  @Test
  public void renamingError() {
    resolveNamesClass(
        "\\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class C \\extends B \\renaming A \\to A' {\n" +
        "  \\abstract a' : A\n" +
        "}", 1);
  }

  @Test
  public void renamingUnknown() {
    resolveNamesClass("test",
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming X \\to X' {}", 1);
  }

  @Test
  public void nameClashError() {
    resolveNamesClass("test",
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming A \\to A' {\n" +
        "  \\abstract A' : \\Set1\n" +
        "}", 1);
  }

  @Test
  public void duplicateName() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming a \\to a' {\n" +
        "  \\abstract a : A\n" +
        "  \\abstract p : a' = a\n" +
        "}\n" +
        "\\function f (c : C) : c.a' = c.a => c.p\n" +
        "\\function g => \\new C { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }");
  }

  @Test
  public void duplicateNameError() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming a \\to a' {\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\function f => \\new C { A => Nat | a => 0 }", 1);
  }

  @Test
  public void duplicateNameError2() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming a \\to a' {\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\function f => \\new C { A => Nat | a' => 0 }", 1);
  }

  @Test
  public void renamingInherited() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming a \\to a' {}\n" +
        "\\static \\class D \\extends C {}\n" +
        "\\function f (d : D) => d.a'");
  }

  @Test
  public void renamingInheritedError() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming a \\to a' {}\n" +
        "\\static \\class D \\extends C {}\n" +
        "\\function f (d : D) => d.a", 1);
  }

  @Test
  public void renamingInherited2() {
    typeCheckClass(
        "\\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a0 : A\n" +
        "}\n" +
        "\\class C \\extends B \\renaming A \\to A', a0 \\to a {\n" +
        "  \\abstract a' : A'\n" +
        "  \\abstract a'' : A'\n" +
        "}\n" +
        "\\class D \\extends C \\renaming A' \\to B, a \\to b, a' \\to b' {\n" +
        "  \\abstract b'' : B\n" +
        "  \\abstract p : b = b'\n" +
        "  \\abstract q : b' = b''\n" +
        "  \\abstract r : a'' = b''\n" +
        "}\n");
  }

  @Test
  public void renamingMultipleInherited() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming A \\to A' {\n" +
        "  \\abstract a' : A'\n" +
        "  \\abstract p : a = a'\n" +
        "}\n" +
        "\\static \\class D \\extends B \\renaming a \\to a' {}\n" +
        "\\static \\class E\n" +
        "  \\extends C\n" +
        "  \\extends D \\renaming a' \\to a''\n" +
        "{}\n" +
        "\\function f (b : B) => b.a\n" +
        "\\function g (e : E) : e.a'' = e.a' => e.p\n" +
        "\\function h (e : E) : f e = e.a' => e.p\n");
  }

  @Test
  public void renamingMultipleInheritedError() {
    resolveNamesClass("test",
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming A \\to A' {\n" +
        "  \\abstract a' : A'\n" +
        "  \\abstract p : a = a'\n" +
        "}\n" +
        "\\static \\class D \\extends B \\renaming a \\to a' {}\n" +
        "\\static \\class E \\extends C, D {}", 1);
  }
}
