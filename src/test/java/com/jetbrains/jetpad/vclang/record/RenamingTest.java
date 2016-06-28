package com.jetbrains.jetpad.vclang.record;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;

public class RenamingTest {
  @Test
  public void renaming() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class B \\extends A \\renaming A \\to A' {\n" +
        "  \\abstract a' : A'\n" +
        "  \\abstract p : a = a'\n" +
        "}\n" +
        "\\static \\function f (b : B) : \\Sigma (x : b.A') (x = b.a') => (b.a, b.p)");
  }
}
