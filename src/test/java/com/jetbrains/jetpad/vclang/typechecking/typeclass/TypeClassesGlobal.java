package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesGlobal extends TypeCheckingTestCase {
  @Test
  public void inferInstance() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> \\Type0" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\static \\instance Nat-X => \\new Y { A => Nat | B => \\lam n => Nat }");
  }
}
