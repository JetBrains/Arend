package com.jetbrains.jetpad.vclang.typeclass;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;

public class TypeClassesNameResolver {
  @Test
  public void resolveNames() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\abstract T : \\Type0\n" +
        "  \\abstract f : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X \\by T { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNames2() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\abstract T : \\Type0\n" +
        "  \\abstract f : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X \\by T { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNamesNonImplicit() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\abstract T : \\Type0\n" +
        "  \\abstract f : \\Type0\n" +
        "  \\abstract h : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X \\by T { f }\n" +
        "\\function g => h", 1);
  }

  @Test
  public void resolveNamesDuplicate() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\abstract T : \\Type0\n" +
        "  \\abstract f : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X \\by T { f }\n" +
        "\\static \\class Y {\n" +
        "  \\abstract T : \\Type0\n" +
        "  \\abstract g : \\Type0 -> \\Type0\n" +
        "}\n" +
        "\\static \\view \\on Y \\by T { g => f }", 1);
  }

  @Test
  public void resolveNamesInner() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\static \\class Z {\n" +
        "    \\abstract T : \\Type0\n" +
        "    \\abstract f : \\Type0\n" +
        "  }\n" +
        "  \\static \\view \\on Z \\by T { f }\n" +
        "}\n" +
        "\\function g => f", 1);
  }

  @Test
  public void resolveNamesRecursive() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\static \\class Z {\n" +
        "    \\abstract T : \\Type0\n" +
        "    \\abstract f : \\Type0\n" +
        "  }\n" +
        "  \\static \\view \\on Z \\by T { f }\n" +
        "}\n" +
        "\\static \\class Y {\n" +
        "  \\abstract T : \\Type0\n" +
        "  \\abstract z : X.Z\n" +
        "}\n" +
        "\\static \\view \\on Y \\by T { z }\n" +
        "\\function g => f", 1);
  }
}
