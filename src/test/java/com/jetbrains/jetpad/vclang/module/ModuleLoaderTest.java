package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import org.junit.Before;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\static \\function f => B.g");
    sourceSupplier.add(moduleB, "\\static \\function g => A.f");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertTrue(moduleLoader.getErrors().size() > 0);
  }

  @Test
  public void recursiveTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\static \\function f => B.g");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertTrue(moduleLoader.getErrors().size() > 0);
  }

  @Test
  public void recursiveTestError3() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\static \\function f => B.g \\static \\function h => 0");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertTrue(moduleLoader.getErrors().size() > 0);
  }

  @Test
  public void nonStaticTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\function h => f");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void staticAbstractTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module module = new Module(moduleLoader.getRoot(), "A");
    sourceSupplier.add(module, "\\static \\function f : Nat");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(module, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void moduleTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    sourceSupplier.add(moduleA, "\\function f : Nat \\static \\class C { \\function g : Nat \\function h => g }");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(1, moduleLoader.getRoot().getChild(new Utils.Name("A")).getMembers().size());
    assertEquals(1, ((ClassDefinition) moduleLoader.getRoot().getMember("A")).getLocalNamespace().getMembers().size());
    assertEquals(0, moduleLoader.getRoot().getMember("A").getNamespace().getMember("C").getNamespace().getMembers().size());
    assertEquals(2, ((ClassDefinition) moduleLoader.getRoot().getMember("A").getNamespace().getMember("C")).getLocalNamespace().getMembers().size());
  }

  @Test
  public void nonStaticTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\static \\class B { \\function g : Nat \\function h => g }");
    sourceSupplier.add(moduleB, "\\static \\function f (p : A.B) => p.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void nonStaticTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class B { \\function g : Nat \\static \\function (+) (f g : Nat) => f \\function h => f + g }");
    sourceSupplier.add(moduleB, "\\static \\function f (p : A.B) => p.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void abstractNonStaticTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat");
    sourceSupplier.add(moduleB, "\\function g => A.f");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void numberOfFieldsTest() {
    ClassDefinition result = parseDefs(dummyModuleLoader, "\\static \\class Point { \\function x : Nat \\function y : Nat } \\static \\function C => Point { \\override x => 0 }");
    assertEquals(2, result.getNamespace().getMembers().size());
    assertEquals(0, result.getLocalNamespace().getMembers().size());
    assertEquals(0, result.getNamespace().getChild(new Utils.Name("Point")).getMembers().size());
    assertEquals(2, ((ClassDefinition) result.getNamespace().getMember("Point")).getLocalNamespace().getMembers().size());
  }

  @Test
  public void openTest() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\static \\function x => 0 } \\open A \\static \\function y => x");
  }

  @Test
  public void closeTestError() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\static \\function x => 0 } \\open A \\static \\function y => x \\close A(x) \\function z => x", 1, 0);
  }

  @Test
  public void exportTest() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\export B } \\static \\function y => A.x");
  }

  @Test
  public void staticFieldAccCallTest() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\function x : Nat \\class B { \\static \\function y => x } } \\static \\function f (a : A) => a.B.y");
  }

  @Test
  public void exportPublicFieldsTest() {
    ClassDefinition result = parseDefs(dummyModuleLoader, "\\static \\class A { \\function x : Nat \\class B { \\static \\function y => x } \\export B } \\static \\function f (a : A) => a.y");
    assertEquals(2, result.getFields().size());
    assertTrue(result.getField("A") instanceof ClassDefinition);
    assertEquals(3, ((ClassDefinition) result.getField("A")).getFields().size());
    assertTrue(result.getField("A").getNamespace().getChildren().isEmpty());
  }

  @Test
  public void nonStaticClassExportTestError() {
    parseDefs(dummyModuleLoader, "\\class A { } \\static \\class B { \\export A }", 1, 0);
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
            "\\static \\function z => x + y\n" +
            "\\static \\function w => x\n" +
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
    assertTrue(classA.getNamespace().getChildren().isEmpty());
    assertTrue(classA.getField("B") instanceof ClassDefinition);
    ClassDefinition classB = (ClassDefinition) classA.getField("B");
    assertEquals(4, classB.getFields().size());
    assertEquals(1, classB.getNamespace().getChildren().size());
    assertTrue(classB.getField("C") instanceof ClassDefinition);
    ClassDefinition classC = (ClassDefinition) classB.getField("C");
    assertEquals(2, classC.getFields().size());
    assertEquals(2, classC.getNamespace().getChildren().size());
    assertEquals(classC.getField("w"), classB.getNamespace().getMember("w"));
    assertTrue(classA.getField("D") instanceof ClassDefinition);
    ClassDefinition classD = (ClassDefinition) classA.getField("D");
    assertEquals(1, classD.getFields().size());
    assertEquals(1, classD.getNamespace().getChildren().size());
    assertEquals(classC.getField("w"), classD.getNamespace().getMember("w"));
    assertEquals(classC.getField("w"), classD.getField("w"));
  }

  @Test
  public void neverCloseField() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\static \\function x => 0 } \\static \\class B { \\open A \\export A \\close A } \\static \\class C { \\static \\function y => B.x }");
  }

  @Test
  public void exportExistingTestError() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\static \\class B { \\static \\function x => 0 } } \\export A \\static \\class B { \\static \\function y => 0 }", 1, 0);
  }

  @Test
  public void exportExistingTestError2() {
    parseDefs(dummyModuleLoader, "\\static \\class B { \\static \\function y => 0 } \\static \\class A { \\static \\class B { \\static \\function x => 0 } } \\export A", 1, 0);
  }

  @Test
  public void openExportTestError() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\open B } \\static \\function y => A.x", 1, 0);
  }

  @Test
  public void export2TestError() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\export B } \\static \\function y => x", 1, 0);
  }

  @Test
  public void openAbstractTestError() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\function x : Nat } \\open A \\function y => x", 1, 0);
  }

  @Test
  public void openAbstractTestError2() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\function x : Nat \\function y => x } \\open A \\function z => y", 1, 0);
  }

  @Test
  public void staticInOnlyStaticTestError() {
    parseDefs(dummyModuleLoader, "\\function B : \\Type0 \\static \\class A {} \\static \\class A { \\static \\function s => 0 \\static \\data D (A : Nat) | foo Nat | bar }", 1, 0);
  }

  @Test
  public void classExtensionWhereTestError() {
    parseDefs(dummyModuleLoader, "\\static \\function f => 0 \\where \\static \\class A {} \\static \\class A { \\function x => 0 }", 1, 0);
  }

  @Test
  public void multipleDefsWhere() {
    parseDefs(dummyModuleLoader, "\\static \\function f => 0 \\where \\static \\function d => 0 \\static \\function d => 1", 1, 0);
  }

  @Test
  public void overrideWhere() {
    parseDefs(dummyModuleLoader, "\\static \\class A { \\function x => 0 } \\static \\function C => A { \\override x => y \\where \\function y => 0 }");
  }
}
