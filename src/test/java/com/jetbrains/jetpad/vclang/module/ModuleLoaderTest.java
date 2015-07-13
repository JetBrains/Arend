package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parse;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static org.junit.Assert.*;

public class ModuleLoaderTest {
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
    assertEquals(2, ((ClassDefinition) moduleLoader.rootModule().getStaticField("A")).getPublicFields().size());
    assertTrue(moduleLoader.rootModule().getStaticField("A").getStaticField("C").getStaticFields() == null || moduleLoader.rootModule().getStaticField("A").getStaticField("C").getStaticFields().isEmpty());
    assertEquals(2, ((ClassDefinition) moduleLoader.rootModule().getStaticField("A").getStaticField("C")).getPublicFields().size());
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
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition result = parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 }");
    assertNotNull(result.getStaticFields());
    assertEquals(2, result.getStaticFields().size());
    assertNotNull(result.getPublicFields());
    assertEquals(2, result.getPublicFields().size());
  }

  @Test
  public void openAbstractTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text = "\\class A { \\function x : Nat } \\open A \\function y => x";
    new BuildVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader).visitDefs(parse(moduleLoader, text).defs());
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void openAbstractTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text = "\\class A { \\function x : Nat \\function y => x } \\open A \\function z => y";
    new BuildVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader).visitDefs(parse(moduleLoader, text).defs());
    assertEquals(1, moduleLoader.getErrors().size());
  }
}
