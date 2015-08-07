package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import org.junit.Before;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static org.junit.Assert.*;

public class ModuleLoaderTest {
  ModuleLoader dummyModuleLoader;

  @Before
  public void initialize() {
    dummyModuleLoader = new ModuleLoader();
    dummyModuleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
  }

  @Test
  public void recursiveTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f => B.g");
    sourceSupplier.add(moduleB, "\\function g => A.f");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertTrue(moduleLoader.getErrors().size() > 0);
  }

  @Test
  public void recursiveTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f => B.g");
    sourceSupplier.add(moduleB, "\\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertTrue(moduleLoader.getErrors().size() > 0);
  }

  @Test
  public void recursiveTestError3() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f => B.g \\function h => 0");
    sourceSupplier.add(moduleB, "\\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertTrue(moduleLoader.getErrors().size() > 0);
  }

  @Test
  public void nonStaticTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\function h => f");
    sourceSupplier.add(moduleB, "\\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void moduleTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class C { \\function g : Nat \\function h => g }");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertNotNull(moduleLoader.rootModule().getStaticField("A").getStaticFields());
    assertEquals(1, moduleLoader.rootModule().getStaticField("A").getStaticFields().size());
    assertEquals(2, moduleLoader.rootModule().getStaticField("A").getFields().size());
    assertTrue(moduleLoader.rootModule().getStaticField("A").getStaticField("C").getStaticFields() == null || moduleLoader.rootModule().getStaticField("A").getStaticField("C").getStaticFields().isEmpty());
    assertEquals(2, moduleLoader.rootModule().getStaticField("A").getStaticField("C").getFields().size());
  }

  @Test
  public void nonStaticTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class B { \\function g : Nat \\function h => g }");
    sourceSupplier.add(moduleB, "\\function f (p : A.B) => p.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void nonStaticTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class B { \\function g : Nat \\function (+) (f g : Nat) => f \\function h => f + g }");
    sourceSupplier.add(moduleB, "\\function f (p : A.B) => p.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void abstractNonStaticTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat");
    sourceSupplier.add(moduleB, "\\function g => A.f");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void numberOfFieldsTest() {
    ClassDefinition result = parseDefs(dummyModuleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 }");
    assertNotNull(result.getStaticFields());
    assertEquals(2, result.getStaticFields().size());
    assertNotNull(result.getFields());
    assertEquals(2, result.getFields().size());
  }

  @Test
  public void openTest() {
    parseDefs(dummyModuleLoader, "\\class A { \\function x : Nat => 0 } \\open A \\function y => x");
  }

  @Test
  public void closeTestError() {
    parseDefs(dummyModuleLoader, "\\class A { \\function x : Nat => 0 } \\open A \\function y => x \\close A(x) \\function z => x", 1, 0);
  }

  @Test
  public void exportTest() {
    parseDefs(dummyModuleLoader, "\\class A { \\class B { \\function x : Nat => 0 } \\export B } \\function y => A.x");
  }

  @Test
  public void openExportTestError() {
    parseDefs(dummyModuleLoader, "\\class A { \\class B { \\function x : Nat => 0 } \\open B } \\function y => A.x", 1, 0);
  }

  @Test
  public void export2TestError() {
    parseDefs(dummyModuleLoader, "\\class A { \\class B { \\function x : Nat => 0 } \\export B } \\function y => x", 1, 0);
  }

  @Test
  public void openAbstractTestError() {
    parseDefs(dummyModuleLoader, "\\class A { \\function x : Nat } \\open A \\function y => x", 1, 0);
  }

  @Test
  public void openAbstractTestError2() {
    parseDefs(dummyModuleLoader, "\\class A { \\function x : Nat \\function y => x } \\open A \\function z => y", 1, 0);
  }

  @Test
  public void staticInOnlyStaticTest() {
    parseDefs(dummyModuleLoader, "\\function B : \\Type0 \\class A {} \\class A { \\function s => 0 \\data D (A : Nat) | foo Nat | bar }");
  }

  @Test
  public void nonStaticInOnlyStaticTestError() {
    parseDefs(dummyModuleLoader, "\\function B : \\Type0 \\class A {} \\class A { \\data D (A : Nat) | foo Nat | bar B }", 1, 0);
  }

  @Test
  public void classExtensionWhere() {
    parseDefs(dummyModuleLoader, "\\function f => 0 \\where \\class A {} \\class A { \\function x => 0 }");
  }

  @Test
  public void multipleDefsWhere() {
    parseDefs(dummyModuleLoader, "\\function f => 0 \\where \\function d => 0 \\function d => 0", 1, 0);
  }

  @Test
  public void overrideWhere() {
    parseDefs(dummyModuleLoader, "\\class A { \\function x => 0 } \\function C => A { \\override x => y \\where \\function y => 0 }", 1, 0);
  }
}
