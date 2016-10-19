package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesTypeChecking extends TypeCheckingTestCase {
  @Test
  public void classNotInScope() {
    typeCheckClass("\\view Foo \\on X \\by X { }", 1, 0);
  }

  @Test
  public void classViewFieldNotInScope() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "}" +
        "\\view Foo \\on X \\by A { B }", 1, 0);
  }

  @Test
  public void classifyingFieldNotInScope() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "}" +
        "\\view Foo \\on X \\by Y { }", 1, 0);
  }
}
