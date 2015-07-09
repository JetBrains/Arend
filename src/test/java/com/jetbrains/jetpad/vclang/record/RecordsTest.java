package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;

public class RecordsTest {
  @Test
  public void recordTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class B { \\function f : Nat -> \\Type0 \\function g : f 0 } \\function f (p : B) : p.f 0 => p.g ");
  }

  @Test
  public void unknownExtTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 \\override z => 0 \\override y => 0 }", 1);
  }

  @Test
  public void typeMismatchMoreTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x (a : Nat) => a }", 1);
  }

  @Test
  public void typeMismatchLessTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class C { \\function f (x y z : Nat) : Nat } \\function D => C { \\override f a => \\lam z w => z }");
  }

  @Test
  public void argTypeMismatchTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class C { \\function f (a : Nat) : Nat } \\function D => C { \\override f (a : Nat -> Nat) => 0 }", 1);
  }

  @Test
  public void resultTypeMismatchTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => \\lam (t : Nat) => t }", 1);
  }

  @Test
  public void parentCallTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader,
        "\\class A {\n" +
            "\\function c : Nat -> Nat -> Nat\n" +
            "\\function f : Nat -> Nat\n" +
            "}\n" +
            "\\function B => A {\n" +
            "\\override f n <= c n n\n" +
            "}");
  }

  @Test
  public void recursiveTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class A { \\function f : Nat -> Nat } \\function B => A { \\override f n <= \\elim n | zero => zero | suc n' => f (suc n') }", 1);
  }

  @Test
  public void duplicateNameTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
            "\\function f : Nat -> Nat\n" +
        "}\n" +
        "\\function B => A {\n" +
            "\\function f (n : Nat) <= n\n" +
        "}";
    parseDefs(moduleLoader, text, 1, 0);
  }

  @Test
  public void overriddenFieldAccTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function diagonal => \\lam (d : Nat) => Point {\n" +
          "\\override x => d\n" +
          "\\override y => d\n" +
        "}\n" +
        "\\function test (p : diagonal 0) : p.x = 0 => path (\\lam _ => 0)";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void newAbstractTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function diagonal => Point {\n" +
          "\\override y => x\n" +
        "}\n" +
        "\\function test => \\new diagonal";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void newTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function diagonal => \\lam (d : Nat) => Point {\n" +
          "\\override x => d\n" +
          "\\override y => d\n" +
        "}\n" +
        "\\function diagonal1 => Point {\n" +
          "\\override x => 0\n" +
          "\\override y => x\n" +
        "}\n" +
        "\\function test : \\new diagonal1 = \\new diagonal 0 => path (\\lam _ => \\new diagonal 0)";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void mutualRecursionTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function test => Point {\n" +
          "\\override x => y\n" +
          "\\override y => x\n" +
        "}";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void splitClassTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
          "\\function x : Nat\n" +
        "}\n" +
        "\\class A {\n" +
          "\\function y => 0\n" +
        "}";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void splitClassTest2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
          "\\function x => 0\n" +
        "}\n" +
        "\\class A {\n" +
          "\\function y : Nat\n" +
        "}";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void splitClassTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
          "\\function x : Nat\n" +
        "}\n" +
        "\\class A {\n" +
          "\\function y : Nat\n" +
        "}";
    parseDefs(moduleLoader, text, 1);
  }
}
