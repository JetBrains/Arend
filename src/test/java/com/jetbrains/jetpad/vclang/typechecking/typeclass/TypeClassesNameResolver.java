package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import org.junit.Test;

public class TypeClassesNameResolver extends NameResolverTestCase {
  @Test
  public void classNotInScope() {
    resolveNamesModule("\\view Foo \\on X \\by X { }", 1);
  }

  @Test
  public void resolveNames() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type0\n" +
        "  | f : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNames2() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type0\n" +
        "  | f : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\function g => f");
  }

  @Test
  public void resolveNamesNonImplicit() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type0\n" +
        "  | f : \\Type0\n" +
        "  | h : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\function g => h", 1);
  }

  @Test
  public void resolveNamesDuplicate() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type0\n" +
        "  | f : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\class Y {\n" +
        "  | T : \\Type0\n" +
        "  | g : \\Type0 -> \\Type0\n" +
        "}\n" +
        "\\view Y' \\on Y \\by T { g => f }", 1);
  }

  @Test
  public void resolveNamesInner() {
    resolveNamesModule(
        "\\class X \\where {\n" +
        "  \\class Z {\n" +
        "    | T : \\Type0\n" +
        "    | f : \\Type0\n" +
        "  }\n" +
        "  \\view Z' \\on Z \\by T { f }\n" +
        "}\n" +
        "\\function g => f", 1);
  }

  @Test
  public void resolveNamesRecursive() {
    resolveNamesModule(
        "\\class X \\where {\n" +
        "  \\class Z {\n" +
        "    | T : \\Type0\n" +
        "    | f : \\Type0\n" +
        "  }\n" +
        "  \\view Z' \\on Z \\by T { f }\n" +
        "}\n" +
        "\\class Y {\n" +
        "  | T : \\Type0\n" +
        "  | z : X.Z\n" +
        "}\n" +
        "\\view Y' \\on Y \\by T { z }\n" +
        "\\function g => f", 1);
  }

  @Test
  public void resolveClassExt() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type1\n" +
        "  | f : \\Type1\n" +
        "}\n" +
        "\\view Y \\on X \\by T { f => g }\n" +
        "\\function h => \\new Y { T => \\Type0 | g => \\Type0 }");
  }

  @Test
  public void resolveClassExtSameName() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type1\n" +
        "  | f : \\Type1\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f => g }\n" +
        "\\function h => \\new X' { T => \\Type0 | g => \\Type0 }");
  }

  @Test
  public void resolveClassExtSameName2() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type1\n" +
        "  | f : \\Type1\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f => g }\n" +
        "\\function h => \\new X' { T => \\Type0 | f => \\Type0 }", 1);
  }

  @Test
  public void duplicateClassView() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type0\n" +
        "  | f : \\Type0\n" +
        "}\n" +
        "\\class Y {\n" +
        "  | T : \\Type0\n" +
        "  | f : \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by T { f }\n" +
        "\\view Y' \\on Y \\by T { f }", 1);
  }

  @Test
  public void duplicateClassViewFieldName() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | T : \\Type0\n" +
        "  | f : \\Type0\n" +
        "}\n" +
        "\\function f => 0\n" +
        "\\view X' \\on X \\by T { f }", 1);
  }

  @Test
  public void cyclicView() {
    resolveNamesModule("\\view X \\on X \\by X { }", 1);
  }

  @Test
  public void instanceWithoutView() {
    resolveNamesModule(
      "\\class X {\n" +
      "  | A : \\Type0\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\data D\n" +
      "\\instance D-X => \\new X { A => D | B => \\lam n => D }", 1);
  }

  @Test
  public void instanceNotView() {
    resolveNamesModule(
      "\\class X {\n" +
      "  | A : \\Type0\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\view X' \\on X \\by A { B }\n" +
      "\\data D\n" +
      "\\instance D-X => \\new X { A => D | B => \\lam _ => D }", 1);
  }
}
