package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesTypeChecking extends TypeCheckingTestCase {
  @Test
  public void classViewFieldNotInScope() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "}\n" +
        "\\view Foo \\on X \\by A { B }", 1, 0);
  }

  @Test
  public void classifyingFieldNotInScope() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "}\n" +
        "\\view Foo \\on X \\by Y { }", 1, 0);
  }

  @Test
  public void instanceNotView() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new X { A => Nat | B => \\lam _ => Nat }", 1);
  }
}
