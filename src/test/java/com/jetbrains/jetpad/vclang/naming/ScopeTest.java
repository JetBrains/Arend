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
    resolveNamesModule("\\class A \\where { \\class B \\where { \\func x => 0 } \\open B } \\func y => A.x", 1);
  }

  @Ignore
  @Test
  public void staticClassExportTest() {
    resolveNamesModule("\\class A \\where { \\func x => 0 } \\class B \\where { \\export A } \\func y => B.x");
  }

  @Ignore
  @Test
  public void nonStaticClassExportTestError() {
    resolveNamesModule("\\class Test { \\class A \\where { \\func x => 0 } } \\where { \\class B \\where { \\export A } \\func y => B.x }", 1);
  }

  @Test
  public void nameResolverPiOpenError() {
    resolveNamesExpr("\\Pi (A : Nat -> \\Type0) (a b : A a) -> A 0", 1);
  }

  @Test
  public void fieldsAreOpen() {
    resolveNamesModule("\\class Test { \\func y => x } \\where { \\class A { | x : Nat } }");
  }

  @Test
  public void dynamicsAreNotOpen() {
    resolveNamesModule("\\class Test { \\func z => y } \\where { \\class A { | x : Nat \\func y => x } }", 1);
  }

  @Test
  public void staticsAreNotOpen() {
    resolveNamesModule("\\class Test { \\func z => y } \\where { \\class A { | x : Nat } \\where { \\func y => 0 } }", 1);
  }

  @Test
  public void openStatics() {
    resolveNamesModule("\\class Test { \\func z => y } \\where { \\class A { | x : Nat } \\where { \\func y => 0 } \\open A }");
  }

  @Test
  public void openDynamics() {
    resolveNamesModule("\\class Test { \\func z => y } \\where { \\class A { | x : Nat \\func y => x } \\open A }");
  }

  @Test
  public void whereError() {
    resolveNamesModule(
        "\\func f (x : Nat) => x \\where\n" +
        "  \\func b => x", 1);
  }

  @Test
  public void whereNoOpenFunctionError() {
    resolveNamesModule(
        "\\func f => x \\where\n" +
        "  \\func b => 0 \\where\n" +
        "    \\func x => 0", 1);
  }

  @Ignore
  @Test
  public void export2TestError() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\func x => 0 } \\export B } \\func y => x", 1);
  }

  @Test
  public void dynamicFunctionTest() {
    resolveNamesModule("\\class A { \\func x => 0 } \\func y : Nat => x", 1);
  }

  @Test
  public void dynamicFunctionOpenTest() {
    resolveNamesModule("\\class A { \\func x => 0 } \\func y : Nat => x \\open A");
  }

  @Test
  public void duplicateInternalName() {
    resolveNamesModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\data D | x Nat", 1);
  }
}
