package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import org.junit.Before;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import org.junit.Test;

import java.util.Collection;

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
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class A { \\class B { \\function x : Nat => 0 } \\export B } \\function y => A.x");
  }

  @Test
  public void exportPublicFieldsTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition result = parseDefs(moduleLoader, "\\class A { \\function x : Nat \\class B { \\function y => x } \\export B } \\function f (a : A) => a.y");
    assertEquals(2, result.getFields().size());
    assertTrue(result.getField("A") instanceof ClassDefinition);
    assertEquals(3, result.getField("A").getFields().size());
    Collection<Definition> staticFields = result.getField("A").getStaticFields();
    assertTrue(staticFields == null || staticFields.size() == 0);
  }

  @Test
  public void exportTest2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition result = parseDefs(moduleLoader,
      "\\function (+) (x y : Nat) : Nat\n" +
      "\\class A {\n" +
        "\\function x : Nat\n" +
        "\\class B {\n" +
          "\\function y : Nat\n" +
          "\\class C {\n" +
            "\\function z => x + y\n" +
            "\\function w => x\n" +
          "}\n" +
          "\\export C\n" +
        "}\n" +
        "\\class D { \\export B }\n" +
        "\\function f (b : B) : b.C.z = x + b.y => path (\\lam _ => x + b.y)\n" +
      "}");
    assertEquals(2, result.getFields().size());
    assertTrue(result.getField("A") instanceof ClassDefinition);
    ClassDefinition classA = (ClassDefinition) result.getField("A");
    assertEquals(4, classA.getFields().size());
    assertTrue(classA.getStaticFields() == null || classA.getStaticFields().size() == 0);
    assertTrue(classA.getField("B") instanceof ClassDefinition);
    ClassDefinition classB = (ClassDefinition) classA.getField("B");
    assertEquals(4, classB.getFields().size());
    assertEquals(1, classB.getStaticFields().size());
    assertTrue(classB.getField("C") instanceof ClassDefinition);
    ClassDefinition classC = (ClassDefinition) classB.getField("C");
    assertEquals(2, classC.getFields().size());
    assertEquals(2, classC.getStaticFields().size());
    assertEquals(classC.getField("w"), classB.getStaticField("w"));
    assertTrue(classA.getField("D") instanceof ClassDefinition);
    ClassDefinition classD = (ClassDefinition) classA.getField("D");
    assertEquals(1, classC.getFields().size());
    assertEquals(1, classC.getStaticFields().size());
    assertEquals(classC.getField("w"), classD.getStaticField("w"));
    assertEquals(classC.getField("z"), classD.getField("z"));
  }

  @Test
  public void neverCloseField() {
    parseDefs(dummyModuleLoader, "\\class A { \\function x => 0 } \\class B { \\open A \\export A \\close A \\function y => x }");
  }

  @Test
  public void exportExistingTestError() {
    parseDefs(dummyModuleLoader, "\\class A { \\class B { \\function x => 0 } } \\export A \\class B { \\function y => 0 }", 1, 0);
  }

  @Test
  public void exportExistingTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class B { \\function y => 0 } \\class A { \\class B { \\function x => 0 } } \\export A", 1, 0);
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
