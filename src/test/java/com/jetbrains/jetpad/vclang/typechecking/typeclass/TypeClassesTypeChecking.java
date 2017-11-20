package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.error.CycleError;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;

public class TypeClassesTypeChecking extends TypeCheckingTestCase {
  @Test
  public void classViewFieldNotInScope() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "}\n" +
        "\\view Foo \\on X \\by A { B }", 1);
  }

  @Test
  public void classifyingFieldNotInScope() {
    resolveNamesModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "}\n" +
        "\\view Foo \\on X \\by Y { }", 1);
  }

  @Test
  public void classViewExt() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B => C }\n" +
        "\\function f => \\new X  { A => Nat | B => \\lam _ => Nat }\n" +
        "\\function g => \\new X' { A => Nat | C => \\lam _ => Nat }\n" +
        "\\function p : f = g => path (\\lam _ => f)");
  }

  @Test
  public void notImplementedField() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\instance x : X' | A => Nat", 1);
  }

  @Test
  public void mutuallyRecursiveInstance() {
    typeCheckModule(
      "\\view X' \\on X \\by A { B }\n" +
      "\\default \\instance Nat-X : X' | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\instance D-X : X' | A => D | B => \\lam _ => f\n" +
      "\\function g {x : X' { A => Nat }} => \\Prop\n" +
      "\\function f => g\n" +
      "\\class X {\n" +
      "  | A : \\Type0\n" +
      "  | B : A -> \\Type0\n" +
      "}");
  }

  @Test
  public void mutuallyRecursiveInstanceError() {
    typeCheckModule(
      "\\view X' \\on X \\by A { B }\n" +
      "\\instance Nat-X : X' | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\default \\instance D-X : X' | A => D | B => \\lam _ => f\n" +
      "\\function g {x : X' { A => Nat }} => \\Prop\n" +
      "\\function f : \\Set0 => g\n" +
      "\\class X {\n" +
      "  | A : \\Type0\n" +
      "  | B : A -> \\Type0\n" +
      "}", 1);
    assertThatErrorsAre(instanceOf(CycleError.class));
  }

  @Test
  public void duplicateInstance() {
    typeCheckModule(
      "\\class X {\n" +
      "  | A : \\Type0\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\view Y \\on X \\by A { B }\n" +
      "\\data D\n" +
      "\\instance D-X : Y | A => D | B => \\lam n => D\n" +
      "\\instance D-Y : Y | A => D | B => \\lam n => D -> D", 1);
  }

  @Test
  public void duplicateDefaultInstance() {
    typeCheckModule(
      "\\class X {\n" +
      "  | A : \\Type0\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\view Y \\on X \\by A { B }\n" +
      "\\view Z \\on X \\by A { B => C }\n" +
      "\\data D | c\n" +
      "\\default \\instance D-Y : Y | A => D | B => \\lam n => D -> D\n" +
      "\\default \\instance D-Z : Z | A => D | C => \\lam n => D -> D\n" +
      "\\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
      "\\function g => f c", 1);
  }
}
