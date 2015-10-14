package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingOrdering;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ModuleLoaderTest {
  ListErrorReporter errorReporter;
  ReportingModuleLoader moduleLoader;
  MemorySourceSupplier sourceSupplier;

  @Before
  public void initialize() {
    RootModule.initialize();
    errorReporter = new ListErrorReporter();
    moduleLoader = new ReportingModuleLoader(new ErrorReporter() {
      @Override
      public void report(GeneralError error) {
        if (error.getLevel() != GeneralError.Level.INFO) {
          errorReporter.report(error);
        }
      }
    }, true);
    sourceSupplier = new MemorySourceSupplier(moduleLoader, errorReporter);
  }

  @Test
  public void recursiveTestError() {
    ResolvedName moduleA = new ResolvedName(RootModule.ROOT, "A");
    ResolvedName moduleB = new ResolvedName(RootModule.ROOT, "B");
    sourceSupplier.add(moduleA, "\\static \\function f => B.g");
    sourceSupplier.add(moduleB, "\\static \\function g => A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleA, false).namespaceMember.getResolvedName(), errorReporter);
    assertFalse(errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestError2() {
    ResolvedName moduleA = new ResolvedName(RootModule.ROOT, "A");
    ResolvedName moduleB = new ResolvedName(RootModule.ROOT, "B");
    sourceSupplier.add(moduleA, "\\static \\function f => B.g");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleA, false).namespaceMember.getResolvedName(), errorReporter);
    assertFalse(errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestError3() {
    ResolvedName moduleA = new ResolvedName(RootModule.ROOT, "A");
    ResolvedName moduleB = new ResolvedName(RootModule.ROOT, "B");
    sourceSupplier.add(moduleA, "\\static \\function f => B.g \\static \\function h => 0");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleA, false).namespaceMember.getResolvedName(), errorReporter);
    assertFalse(errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void nonStaticTestError() {
    ResolvedName moduleA = new ResolvedName(RootModule.ROOT, "A");
    ResolvedName moduleB = new ResolvedName(RootModule.ROOT, "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\function h => f");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleB, false).namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void staticAbstractTestError() {
    ResolvedName module = new ResolvedName(RootModule.ROOT, "A");
    sourceSupplier.add(module, "\\static \\function f : Nat");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(module, false).namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void moduleTest() {
    ResolvedName module = new ResolvedName(RootModule.ROOT, "A");
    sourceSupplier.add(module, "\\function f : Nat \\static \\class C { \\function g : Nat \\function h => g }");
    moduleLoader.setSourceSupplier(sourceSupplier);

    ModuleLoadingResult result = moduleLoader.load(module, false);
    TypecheckingOrdering.typecheck(result.namespaceMember.getResolvedName(), errorReporter);
    assertNotNull(result);
    assertEquals(errorReporter.getErrorList().toString(), 0, errorReporter.getErrorList().size());
    assertEquals(2, RootModule.ROOT.getChild(new Name("A")).getMembers().size());
    Definition definitionC = result.namespaceMember.namespace.getDefinition("C");
    assertTrue(definitionC instanceof ClassDefinition);
    assertEquals(2, definitionC.getParentNamespace().findChild(definitionC.getName().name).getMembers().size());
  }

  @Test
  public void nonStaticTest() {
    ResolvedName moduleA = new ResolvedName(RootModule.ROOT, "A");
    ResolvedName moduleB = new ResolvedName(RootModule.ROOT, "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\static \\class B { \\function g : Nat \\function h => g }");
    sourceSupplier.add(moduleB, "\\static \\function f (p : A.B) => p.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleB, false).namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 0, errorReporter.getErrorList().size());
  }

  @Test
  public void nonStaticTestError2() {
    ResolvedName moduleA = new ResolvedName(RootModule.ROOT, "A");
    ResolvedName moduleB = new ResolvedName(RootModule.ROOT, "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\static \\class B { \\function g : Nat \\function h => g }");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class B { \\function g : Nat \\static \\function (+) (f g : Nat) => f \\function h => f + g }");
    sourceSupplier.add(moduleB, "\\static \\function f (p : A.B) => p.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleB, false).namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void abstractNonStaticTestError() {
    ResolvedName moduleA = new ResolvedName(RootModule.ROOT, "A");
    ResolvedName moduleB = new ResolvedName(RootModule.ROOT, "B");
    sourceSupplier.add(moduleA, "\\function f : Nat");
    sourceSupplier.add(moduleB, "\\function g => A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleB, false).namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }
}
