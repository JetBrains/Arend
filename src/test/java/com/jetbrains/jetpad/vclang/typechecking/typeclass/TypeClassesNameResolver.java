package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import org.junit.Test;

public class TypeClassesNameResolver extends NameResolverTestCase {
  @Test
  public void classNotInScope() {
    resolveNamesClass("\\view Foo \\on X \\by X { }", 1);
  }

  @Test
  public void resolveNames() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field f : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNames2() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field f : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNamesNonImplicit() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field f : \\Type0\n" +
        "  \\field h : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\function g => h", 1);
  }

  @Test
  public void resolveNamesDuplicate() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field f : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\class Y {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field g : \\Type0 -> \\Type0\n" +
        "}\n" +
        "\\view Y' \\on Y \\by T { g => f }", 1);
  }

  @Test
  public void resolveNamesInner() {
    resolveNamesClass(
        "\\class X \\where {\n" +
        "  \\class Z {\n" +
        "    \\field T : \\Type0\n" +
        "    \\field f : \\Type0\n" +
        "  }\n" +
        "  \\view Z' \\on Z \\by T { f }\n" +
        "}\n" +
        "\\function g => f", 1);
  }

  @Test
  public void resolveNamesRecursive() {
    resolveNamesClass(
        "\\class X \\where {\n" +
        "  \\class Z {\n" +
        "    \\field T : \\Type0\n" +
        "    \\field f : \\Type0\n" +
        "  }\n" +
        "  \\view Z' \\on Z \\by T { f }\n" +
        "}\n" +
        "\\class Y {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field z : X.Z\n" +
        "}\n" +
        "\\view Y' \\on Y \\by T { z }\n" +
        "\\function g => f", 1);
  }

  @Test
  public void resolveClassExt() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type1\n" +
        "  \\field f : \\Type1\n" +
        "}\n" +
        "\\view Y \\on X \\by T { f => g }\n" +
        "\\function h => \\new Y { T => \\Type0 | g => \\Type0 }");
  }

  @Test
  public void resolveClassExtSameName() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type1\n" +
        "  \\field f : \\Type1\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f => g }\n" +
        "\\function h => \\new X' { T => \\Type0 | g => \\Type0 }");
  }

  @Test
  public void resolveClassExtSameName2() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type1\n" +
        "  \\field f : \\Type1\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f => g }\n" +
        "\\function h => \\new X' { T => \\Type0 | f => \\Type0 }", 1);
  }

  @Test
  public void duplicateClassView() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field f : \\Type0\n" +
        "}\n" +
        "\\class Y {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field f : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\view Y' \\on Y \\by T { f }", 1);
  }

  @Test
  public void duplicateClassViewFieldName() {
    resolveNamesClass(
        "\\class X {\n" +
        "  \\field T : \\Type0\n" +
        "  \\field f : \\Type0\n" +
        "}\n" +
        "\\function f => 0\n" +
        "\\view X' \\on X \\by T { f }", 1);
  }

  @Test
  public void cyclicView() {
    resolveNamesClass("\\view X \\on X \\by X { }", 1);
  }

  @Test
  public void instanceWithoutView() {
    resolveNamesClass(
      "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\instance Nat-X => \\new X { A => Nat | B => \\lam n => Nat }", 1);
  }

  @Test
  public void instanceNotView() {
    resolveNamesClass(
      "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\instance Nat-X => \\new X { A => Nat | B => \\lam _ => Nat }", 1);
  }
}
