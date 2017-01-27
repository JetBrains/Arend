package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

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
      "\\class X {\n" +
      "  \\field A : \\Type0\n" +
      "  \\field B : A -> \\Type0\n" +
      "}\n" +
      "\\view X' \\on X \\by A { B }\n" +
      "\\default \\instance Nat-X => \\new X' { A => Nat | B => \\lam _ => Nat }\n" +
      "\\data D | c\n" +
      "\\instance D-X => \\new X' { A => D | B => \\lam _ => f }\n" +
      "\\function f => B 0");
  }

  @Test
  public void mutuallyRecursiveInstanceError() {
    typeCheckClass(
      "\\class X {\n" +
      "  \\field A : \\Type0\n" +
      "  \\field B : A -> \\Type0\n" +
      "}\n" +
      "\\view X' \\on X \\by A { B }\n" +
      "\\default \\instance Nat-X => \\new X' { A => Nat | B => \\lam _ => Nat }\n" +
      "\\data D | c\n" +
      "\\instance D-X => \\new X' { A => D | B => \\lam _ => f }\n" +
      "\\function f => B c", 1);
  }
}
