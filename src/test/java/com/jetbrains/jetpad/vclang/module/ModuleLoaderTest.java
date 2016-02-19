package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingOrdering;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.module.ModulePath.moduleName;
import static org.junit.Assert.*;

public class ModuleLoaderTest {
  ListErrorReporter errorReporter;
  List<Abstract.Definition> loadedModuleRoots;
  List<ModuleID> loadedModules;
  ReportingModuleLoader moduleLoader;
  MemorySourceSupplier sourceSupplier;
  MemoryOutputSupplier outputSupplier;

  void setupSources() {
    sourceSupplier.add(moduleName("A"), "\\static \\function a => ::B::C::E.e");
    sourceSupplier.add(moduleName("B"), "\\static \\function b => 0");
    sourceSupplier.add(moduleName("B", "C", "D"), "\\static \\function d => 0");
    sourceSupplier.add(moduleName("B", "C", "E"), "\\static \\function e => ::B::C::F.f");
    sourceSupplier.add(moduleName("B", "C", "F"), "\\static \\function f => 0");
    sourceSupplier.add(moduleName("All"), "\\export ::A \\export ::B \\export ::B::C::E \\export ::B::C::D \\export ::B::C::F");
    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.setOutputSupplier(outputSupplier);

    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));

    TypecheckingOrdering.typecheck(loadedModuleRoots, errorReporter);

    for (ModuleID module : loadedModules) {
      moduleLoader.save(module);
    }
    assertTrue(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());

    initializeModuleLoader();
    moduleLoader.setOutputSupplier(outputSupplier);
    moduleLoader.setSourceSupplier(sourceSupplier);
  }

  private void initializeModuleLoader() {
    Root.initialize();
    loadedModules = new ArrayList<>();
    loadedModuleRoots = new ArrayList<>();
    errorReporter = new ListErrorReporter();
    moduleLoader = new ReportingModuleLoader(new ErrorReporter() {
      @Override
      public void report(GeneralError error) {
        if (error.getLevel() != GeneralError.Level.INFO) {
          errorReporter.report(error);
        }
      }

    }, false) {
      @Override
      public void loadingSucceeded(ModuleID module, NamespaceMember namespaceMember, boolean compiled) {
        loadedModules.add(module);
        loadedModuleRoots.add(namespaceMember.abstractDefinition);
      }
    };
  }

  @Before
  public void initialize() {
    initializeModuleLoader();
    sourceSupplier = new MemorySourceSupplier(moduleLoader, errorReporter);
    outputSupplier = new MemoryOutputSupplier();
  }

  @Test
  public void recursiveTestError() {
    sourceSupplier.add(moduleName("A"), "\\static \\function f => ::B.g");
    sourceSupplier.add(moduleName("B"), "\\static \\function g => ::A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(moduleLoader.locateModule(moduleName("A")));
    assertTrue(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestNoError2() {
    sourceSupplier.add(moduleName("A"), "\\static \\function f => ::B.g");
    sourceSupplier.add(moduleName("B"), "\\static \\function g => ::A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(moduleLoader.locateModule(moduleName("A")));
    assertTrue(errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestNoError3() {
    sourceSupplier.add(moduleName("A"), "\\static \\function f => ::B.g \\static \\function h => 0");
    sourceSupplier.add(moduleName("B"), "\\static \\function g => ::A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(moduleLoader.locateModule(moduleName("A")));
    assertTrue(errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void nonStaticTestError() {
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat \\function h => f");
    sourceSupplier.add(moduleName("B"), "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    ModuleLoader.Result result = moduleLoader.load(moduleLoader.locateModule(moduleName("B")));
    TypecheckingOrdering.typecheck(result.namespaceMember.abstractDefinition, errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void staticAbstractTestError() {
    sourceSupplier.add(moduleName("A"), "\\static \\abstract f : Nat");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(moduleLoader.locateModule(moduleName("A")));
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void moduleTest() {
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat \\static \\class C { \\abstract g : Nat \\function h => g }");
    moduleLoader.setSourceSupplier(sourceSupplier);

    ModuleID moduleID = moduleLoader.locateModule(moduleName("A"));
    ModuleLoader.Result result = moduleLoader.load(moduleID);
    TypecheckingOrdering.typecheck(result.namespaceMember.abstractDefinition, errorReporter);
    assertNotNull(result);
    assertEquals(errorReporter.getErrorList().toString(), 0, errorReporter.getErrorList().size());
    assertEquals(2, Root.getModule(moduleID).namespace.getMembers().size());
    Definition definitionC = result.namespaceMember.namespace.getDefinition("C");
    assertTrue(definitionC instanceof ClassDefinition);
    assertEquals(2, definitionC.getParentNamespace().findChild(definitionC.getName()).getMembers().size());
  }

  @Test
  public void nonStaticTest() {
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat \\static \\class B { \\abstract g : Nat \\function h => g }");
    sourceSupplier.add(moduleName("B"), "\\static \\function f (p : A.B) => p.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleLoader.locateModule(moduleName("A"))).namespaceMember.abstractDefinition, errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 0, errorReporter.getErrorList().size());
  }

  @Test
  public void nonStaticTestError2() {
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat \\class B { \\abstract g : Nat \\static \\function (+) (f g : Nat) => f \\function h => f + g }");
    sourceSupplier.add(moduleName("B"), "\\static \\function f (p : A.B) => p.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleLoader.locateModule(moduleName("B"))).namespaceMember.abstractDefinition, errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void abstractNonStaticTestError() {
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat");
    sourceSupplier.add(moduleName("B"), "\\function g => A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleLoader.locateModule(moduleName("B"))).namespaceMember.abstractDefinition, errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  private void assertLoaded(String... path) {
    NamespaceMember member = Root.getModule(moduleLoader.locateModule(moduleName(path)));
    assertTrue(member.definition != null && member.abstractDefinition == null);
  }

  private void assertNotLoaded(String... path) {
    NamespaceMember member = Root.getModule(moduleLoader.locateModule(moduleName(path)));
    assertFalse(member.definition != null && member.abstractDefinition == null);
  }

  @Test
  public void testForTestCase() {
    setupSources();

    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertTrue(errorReporter.getErrorList().isEmpty());
    assertLoaded("All");
    assertLoaded("A");
    assertLoaded("B");
    assertLoaded("B", "C", "E");
    assertLoaded("B", "C", "D");
   }

  @Test
  public void testChange1() {
    setupSources();

    sourceSupplier.touch(moduleName("B"));
    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertTrue(errorReporter.getErrorList().isEmpty());
    assertNotLoaded("All");
    assertLoaded("A");
    assertNotLoaded("B");
    assertLoaded("B", "C", "E");
    assertLoaded("B", "C", "D");
    assertLoaded("B", "C", "F");
  }

  @Test
  public void testChange2() {
    setupSources();

    sourceSupplier.touch(moduleName("B", "C", "D"));
    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertTrue(errorReporter.getErrorList().isEmpty());
    assertNotLoaded("All");
    assertLoaded("A");
    assertLoaded("B");
    assertNotLoaded("B", "C", "D");
    assertLoaded("B", "C", "E");
    assertLoaded("B", "C", "F");
  }

  @Test
  public void testChangeAddNewFile() {
    setupSources();

    sourceSupplier.add(moduleName("B", "C", "G"), "\\static \\function G => f");
    moduleLoader.load(moduleLoader.locateModule(moduleName("B")));
    moduleLoader.load(moduleLoader.locateModule(moduleName("B", "C", "G")));
    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertTrue(errorReporter.getErrorList().isEmpty());
    assertLoaded("All");
    assertLoaded("A");
    assertLoaded("B");
    assertLoaded("B", "C", "D");
    assertLoaded("B", "C", "E");
    assertLoaded("B", "C", "F");
    assertNotLoaded("B", "C", "G");
  }

  @Test
  public void testRemoveOutput() {
    setupSources();

    outputSupplier.remove(moduleName("B"));
    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertTrue(errorReporter.getErrorList().isEmpty());
    assertNotLoaded("All");
    assertLoaded("A");
    assertLoaded("B", "C", "D");
    assertLoaded("B", "C", "E");
    assertLoaded("B", "C", "F");
  }
}
