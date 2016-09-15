package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import org.junit.Test;

public class TypeClassesNameResolver extends NameResolverTestCase {
  @Test
  public void resolveNames() {
    resolveNamesClass(
        "\\static \\class X {\n" +
        "  \\abstract T : \\Type0\n" +
        "  \\abstract f : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X \\by T { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNames2() {
    resolveNamesClass(
        "\\static \\class X {\n" +
        "  \\abstract T : \\Type0\n" +
        "  \\abstract f : \\Type0\n" +
        "}\n" +
        "\\static \\view \\on X \\by T { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNamesNonImplicit() {
    resolveNamesClass(
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
    resolveNamesClass(
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
    resolveNamesClass(
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
    resolveNamesClass(
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
