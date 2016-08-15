package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;

public class HidingTest extends TypeCheckingTestCase {
  @Test
  public void hidingError() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A \\hiding a {\n" +
        "  \\abstract a' : A\n" +
        "  \\abstract p : a = a'\n" +
        "}", 1);
  }

  @Test
  public void hidingError2() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A \\hiding a {\n" +
        "  \\abstract a' : A\n" +
        "}\n" +
        "\\function f (b : B) => b.a", 1);
  }

  @Test
  public void hiding() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class B \\extends A \\hiding A {\n" +
        "  \\abstract p : a = a\n" +
        "}\n" +
        "\\function f (x : A) => x.a = x.a\n" +
        "\\function g (x : B) : f x => x.p");
  }

  @Test
  public void hidingUnknown() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\hiding X {}", 1);
  }

  @Test
  public void hidingRenamingError() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\renaming A \\to A' \\hiding A' \\renaming a \\to a' {}", 1);
  }

  @Test
  public void hidingRenamingError2() {
    resolveNamesClass(
        "\\static \\class B {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class C \\extends B \\hiding A {\n" +
        "  \\abstract a' : A\n" +
        "}", 1);
  }

  @Test
  public void hidingNewError() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class B \\extends A \\hiding a {}\n" +
        "\\function f => \\new B { A => Nat }", 1);
  }

  @Test
  public void hidingNew() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class B \\extends A \\hiding a {}\n" +
        "\\function f => \\new B { A => Nat | a => 0 }");
  }
}
