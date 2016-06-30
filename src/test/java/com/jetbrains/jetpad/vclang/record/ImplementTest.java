package com.jetbrains.jetpad.vclang.record;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;

public class ImplementTest {
  @Test
  public void implementInFunctionError() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\abstract x : Nat\n" +
        "  \\static \\class f => 0\n" +
        "    \\where\n" +
        "      \\implement x => 1\n" +
        "}", 1);
  }

  // TODO: Add tests on the universe of a class
}
