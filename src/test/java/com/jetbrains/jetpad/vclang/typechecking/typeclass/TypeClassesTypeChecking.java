package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.error.local.CycleError;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;

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
  public void classViewExt() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B => C }\n" +
        "\\function f => \\new X  { A => Nat | B => \\lam _ => Nat }\n" +
        "\\function g => \\new X' { A => Nat | C => \\lam _ => Nat }\n" +
        "\\function p : f = g => path (\\lam _ => f)");
  }

  @Test
  public void notImplementedField() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\field A : \\Type0\n" +
        "  \\field B : A -> \\Type0\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\instance x => \\new X' { A => Nat }", 1);
  }

  @Test
  public void mutuallyRecursiveInstance() {
    typeCheckClass(
      "\\view X' \\on X \\by A { B }\n" +
      "\\default \\instance Nat-X => \\new X' { A => Nat | B => \\lam _ => Nat }\n" +
      "\\data D | c\n" +
      "\\instance D-X => \\new X' { A => D | B => \\lam _ => f }\n" +
      "\\function g {x : X' { A => Nat }} => \\Prop\n" +
      "\\function f => g\n" +
      "\\class X {\n" +
      "  \\field A : \\Type0\n" +
      "  \\field B : A -> \\Type0\n" +
      "}");
  }

  @Test
  public void mutuallyRecursiveInstanceError() {
    typeCheckClass(
      "\\view X' \\on X \\by A { B }\n" +
      "\\instance Nat-X => \\new X' { A => Nat | B => \\lam _ => Nat }\n" +
      "\\data D | c\n" +
      "\\default \\instance D-X => \\new X' { A => D | B => \\lam _ => f }\n" +
      "\\function g {x : X' { A => Nat }} => \\Prop\n" +
      "\\function f => g\n" +
      "\\class X {\n" +
      "  \\field A : \\Type0\n" +
      "  \\field B : A -> \\Type0\n" +
      "}", 1);
    assertThatErrorsAre(instanceOf(CycleError.class));
  }

  @Test
  public void duplicateInstance() {
    typeCheckClass(
      "\\class X {\n" +
      "  \\field A : \\Type0\n" +
      "  \\field B : A -> \\Type0\n" +
      "}\n" +
      "\\view Y \\on X \\by A { B }\n" +
      "\\data D\n" +
      "\\instance D-X => \\new Y { A => D | B => \\lam n => D }\n" +
      "\\instance D-Y => \\new Y { A => D | B => \\lam n => D -> D }", 1);
  }

  @Test
  public void duplicateDefaultInstance() {
    typeCheckClass(
      "\\class X {\n" +
      "  \\field A : \\Type0\n" +
      "  \\field B : A -> \\Type0\n" +
      "}\n" +
      "\\view Y \\on X \\by A { B }\n" +
      "\\view Z \\on X \\by A { B => C }\n" +
      "\\data D | c\n" +
      "\\default \\instance D-Y => \\new Y { A => D | B => \\lam n => D -> D }\n" +
      "\\default \\instance D-Z => \\new Z { A => D | C => \\lam n => D -> D }\n" +
      "\\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
      "\\function g => f c", 1);
  }
}
