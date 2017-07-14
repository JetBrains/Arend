package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

public class ScopeTest extends TypeCheckingTestCase {
  @Test
  public void nameResolverLamOpenError() {
    resolveNamesExpr("\\lam (x : Nat) => (\\lam (y : Nat) => \\Pi (z : Nat -> \\Type0) (y : Nat) -> z ((\\lam (y : Nat) => y) y)) y", 1);
  }

  @Test
  public void openExportTestError() {
    resolveNamesClass("\\class A \\where { \\class B \\where { \\function x => 0 } \\open B } \\function y => A.x", 1);
  }

  @Ignore
  @Test
  public void staticClassExportTest() {
    resolveNamesClass("\\class A \\where { \\function x => 0 } \\class B \\where { \\export A } \\function y => B.x");
  }

  @Ignore
  @Test
  public void nonStaticClassExportTestError() {
    resolveNamesClass("\\class Test { \\class A \\where { \\function x => 0 } } \\where { \\class B \\where { \\export A } \\function y => B.x }", 1);
  }

  @Test
  public void nameResolverPiOpenError() {
    resolveNamesExpr("\\Pi (A : Nat -> \\Type0) (a b : A a) -> A 0", 1);
  }

  @Test
  public void openAbstractTestError() {
    resolveNamesClass("\\class Test { \\function y => x } \\where { \\class A { \\field x : Nat } \\open A }", 1);
  }

  @Test
  public void openAbstractTestError2() {
    resolveNamesClass("\\class Test { \\function z => y } \\where { \\class A { \\field x : Nat \\function y => x } \\open A }", 1);
  }

  @Test
  public void whereError() {
    resolveNamesClass(
        "\\function f (x : Nat) => x \\where\n" +
        "  \\function b => x", 1);
  }

  @Test
  public void whereNoOpenFunctionError() {
    resolveNamesClass(
        "\\function f => x \\where\n" +
        "  \\function b => 0 \\where\n" +
        "    \\function x => 0", 1);
  }

  @Ignore
  @Test
  public void export2TestError() {
    resolveNamesClass("\\class A \\where { \\class B \\where { \\function x => 0 } \\export B } \\function y => x", 1);
  }

  @Test
  public void notInScopeTest() {
    resolveNamesClass("\\class A { \\function x => 0 } \\function y : Nat => x", 1);
  }
}
