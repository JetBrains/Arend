package com.jetbrains.jetpad.vclang.typeclass;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;

public class TypeClassesNameResolver {
  @Test
  public void resolveNames() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\abstract f : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNamesNonImplicit() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\abstract f : \\Type0\n" +
        "  \\abstract h : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X { f }\n" +
        "\\function g => h", 1);
  }

  @Test
  public void resolveNamesDuplicate() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\abstract f : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X { f }\n" +
        "\\static \\class Y {\n" +
        "  \\abstract g : \\Type0 -> \\Type0\n" +
        "}\n" +
        "\\static \\view \\on Y { g => f }", 1);
  }

  @Test
  public void resolveNamesInner() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\static \\class Z {\n" +
        "    \\abstract f : \\Type0\n" +
        "  }\n" +
        "  \\static \\view \\on Z { f }\n" +
        "}\n" +
        "\\function g => f", 1);
  }

  @Test
  public void resolveNamesRecursive() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\static \\class Z {\n" +
        "    \\abstract f : \\Type0\n" +
        "  }\n" +
        "  \\static \\view \\on Z { f }\n" +
        "}\n" +
        "\\static \\class Y {\n" +
        "  \\abstract z : X.Z\n" +
        "}\n" +
        "\\static \\view \\on Y { z }\n" +
        "\\function g => f", 1);
  }
}
