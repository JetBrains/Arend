package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;

public class ScopeTest {
  @Test
  public void nameResolverLamOpenError() {
    typeCheckExpr("\\lam (x : Nat) => (\\lam (y : Nat) => \\Pi (z : Nat -> \\Type0) (y : Nat) -> z ((\\lam (y : Nat) => y) y)) y", null, 1);
  }

  @Test
  public void openExportTestError() {
    typeCheckClass("\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\open B } \\static \\function y => A.x", 1);
  }

  @Test
  public void defCallNonStaticTestError() {
    typeCheckClass("\\static \\class A { \\function x => 0 } \\static \\function y => A.x", 1);
  }

  @Test
  public void defCallStaticTestError() {
    typeCheckClass("\\static \\class A { \\static \\function x => 0 } \\static \\function y (a : A) => a.x", 1);
  }

  @Test
  public void defCallInnerNonStaticTestError() {
    typeCheckClass("\\static \\class A { \\class B { \\function x => 0 } } \\static \\function y (a : A) => a.B.x", 1);
  }

  @Test
  public void defCallInnerNonStaticTest() {
    typeCheckClass("\\static \\class A { \\class B { \\static \\function x => 0 } } \\static \\function y (a : A) => a.B.x");
  }

  @Test
  public void staticDefCallTest() {
    typeCheckClass("\\static \\class A { \\static \\function x => 0 } \\static \\function y : Nat => A.x");
  }

  @Test
  public void staticClassExportTest() {
    typeCheckClass("\\static \\class A { \\static \\function x => 0 } \\static \\class B { \\export A } \\static \\function y => B.x");
  }

  @Test
  public void nonStaticClassExportTestError() {
    typeCheckClass("\\class A { \\static \\function x => 0 } \\static \\class B { \\export A } \\static \\function y => B.x", 1);
  }

  @Test
  public void nameResolverPiOpenError() {
    typeCheckExpr("\\Pi (A : Nat -> \\Type0) (a b : A a) -> A 0", null, 1);
  }

  @Test
  public void openAbstractTestError() {
    typeCheckClass("\\static \\class A { \\abstract x : Nat } \\open A \\function y => x", 1);
  }

  @Test
  public void openAbstractTestError2() {
    typeCheckClass("\\static \\class A { \\abstract x : Nat \\function y => x } \\open A \\function z => y", 1);
  }

  @Test
  public void closeTestError() {
    typeCheckClass("\\static \\class A { \\static \\function x => 0 } \\open A \\static \\function y => x \\close A(x) \\function z => x", 1);
  }

  @Test
  public void whereError() {
    typeCheckClass(
        "\\static \\function f (x : Nat) => x \\where\n" +
        "  \\static \\function b => x", 1);
  }

  @Test
  public void whereNoOpenFunctionError() {
    typeCheckClass(
        "\\static \\function f => x \\where\n" +
        "  \\static \\function b => 0 \\where\n" +
        "    \\static \\function x => 0", 1);
  }

  @Test
  public void whereClosedError() {
    typeCheckClass(
        "\\static \\function f => x \\where {\n" +
        "  \\static \\class A { \\static \\function x => 0 }\n" +
        "  \\open A\n" +
        "  \\close A\n" +
        "}", 1);
  }

  @Test
  public void export2TestError() {
    typeCheckClass("\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\export B } \\static \\function y => x", 1);
  }

  @Test
  public void notInScopeTest() {
    typeCheckClass("\\static \\class A { \\function x => 0 } \\static \\function y : Nat => x", 1);
  }
}
