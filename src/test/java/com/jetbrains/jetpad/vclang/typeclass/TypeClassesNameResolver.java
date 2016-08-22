package com.jetbrains.jetpad.vclang.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;

public class TypeClassesNameResolver extends TypeCheckingTestCase {
  @Test
  public void resolveNames() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\implicit \\abstract f : Nat\n" +
        "}\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNamesNonImplicit() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\abstract f : Nat\n" +
        "}\n" +
        "\\function g => f", 1);
  }

  @Test
  public void resolveNamesDuplicate() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\implicit \\abstract f : Nat\n" +
        "}\n" +
        "\\static \\class Y {\n" +
        "  \\implicit \\abstract f : Nat -> Nat\n" +
        "}", 1);
  }

  @Test
  public void resolveNamesInner() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\static \\class Z {\n" +
        "    \\implicit \\abstract f : Nat\n" +
        "  }\n" +
        "}\n" +
        "\\function g => f", 1);
  }

  @Test
  public void resolveNamesRecursive() {
    resolveNamesClass("test",
        "\\static \\class X {\n" +
        "  \\static \\class Z {\n" +
        "    \\implicit \\abstract f : Nat\n" +
        "  }\n" +
        "}\n" +
        "\\static \\class Y {\n" +
        "  \\implicit \\abstract z : X.Z\n" +
        "}\n" +
        "\\function g => f", 1);
  }
}
