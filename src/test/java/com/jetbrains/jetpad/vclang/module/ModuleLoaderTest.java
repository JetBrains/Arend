package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ModuleLoaderTest {
  SimpleModuleLoader moduleLoader;
  MemorySourceSupplier sourceSupplier;

  @Before
  public void initialize() {
    RootModule.initialize();
    moduleLoader = new SimpleModuleLoader(true);
    sourceSupplier = new MemorySourceSupplier(moduleLoader, moduleLoader.getErrorReporter());
  }

  @Test
  public void recursiveTestError() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\static \\function f => B.g");
    sourceSupplier.add(moduleB, "\\static \\function g => A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(RootModule.ROOT, "A", false);
    assertFalse(moduleLoader.getErrorReporter().getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestError2() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\static \\function f => B.g");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(RootModule.ROOT, "A", false);
    assertFalse(moduleLoader.getErrorReporter().getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestError3() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\static \\function f => B.g \\static \\function h => 0");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(RootModule.ROOT, "A", false);
    assertFalse(moduleLoader.getErrorReporter().getErrorList().isEmpty());
  }

  @Test
  public void nonStaticTestError() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\function f : Nat \\function h => f");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(RootModule.ROOT, "B", false);
    assertEquals(1, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void staticAbstractTestError() {
    Namespace module = RootModule.ROOT.getChild(new Utils.Name("A"));
    sourceSupplier.add(module, "\\static \\function f : Nat");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(RootModule.ROOT, "A", false);
    assertEquals(1, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void moduleTest() {
    Namespace module = RootModule.ROOT.getChild(new Utils.Name("A"));
    sourceSupplier.add(module, "\\function f : Nat \\static \\class C { \\function g : Nat \\function h => g }");
    moduleLoader.setSourceSupplier(sourceSupplier);
    ModuleLoadingResult result = moduleLoader.load(RootModule.ROOT, "A", false);
    assertEquals(0, moduleLoader.getErrorReporter().getErrorList().size());
    assertEquals(1, RootModule.ROOT.getChild(new Utils.Name("A")).getDefinitions().size());
    assertEquals(1, result.classDefinition.getLocalNamespace().getDefinitions().size());
    assertEquals(0, result.classDefinition.getNamespace().getDefinition("C").getNamespace().getDefinitions().size());
    assertEquals(2, ((ClassDefinition) result.classDefinition.getNamespace().getDefinition("C")).getLocalNamespace().getDefinitions().size());
  }

  @Test
  public void nonStaticTest() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\function f : Nat \\static \\class B { \\function g : Nat \\function h => g }");
    sourceSupplier.add(moduleB, "\\static \\function f (p : A.B) => p.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(RootModule.ROOT, "B", false);
    assertEquals(0, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void nonStaticTestError2() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\function f : Nat \\static \\class B { \\function g : Nat \\function h => g }");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class B { \\function g : Nat \\static \\function (+) (f g : Nat) => f \\function h => f + g }");
    sourceSupplier.add(moduleB, "\\static \\function f (p : A.B) => p.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(RootModule.ROOT, "B", false);
    assertEquals(1, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void abstractNonStaticTestError() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\function f : Nat");
    sourceSupplier.add(moduleB, "\\function g => A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(RootModule.ROOT, "B", false);
    assertEquals(1, moduleLoader.getErrorReporter().getErrorList().size());
  }
}
