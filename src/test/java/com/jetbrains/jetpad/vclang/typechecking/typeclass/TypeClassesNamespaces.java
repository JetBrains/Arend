package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesNamespaces extends TypeCheckingTestCase {
  @Test
  public void typeClassFullName() {
    typeCheckClass(
        "\\static \\class M {\n" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\static \\view \\on M.X \\by A { B }\n" +
        "\\function f (x : M.X) (a : x.A) => M.B a");
  }

  @Test
  public void typeClassOpen() {
    typeCheckClass(
        "\\static \\class M {" +
        "  \\static \\class X {\n" +
        "    \\abstract A : \\Type0\n" +
        "    \\abstract B : A -> Nat\n" +
        "  }\n" +
        "}\n" +
        "\\open M\n" +
        "\\static \\view \\on X \\by A { B }\n" +
        "\\function f (x : X) (a : x.A) => B a");
  }
}
