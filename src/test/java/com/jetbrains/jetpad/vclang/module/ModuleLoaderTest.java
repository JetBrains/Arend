package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingOrdering;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.module.MemorySourceSupplier.moduleName;
import static org.junit.Assert.*;

public class ModuleLoaderTest {
  ListErrorReporter errorReporter;
  List<ResolvedName> loadedModules;
  ReportingModuleLoader moduleLoader;
  MemorySourceSupplier sourceSupplier;
  MemoryOutputSupplier outputSupplier;

  void setupSources() {
    sourceSupplier.add(moduleName("A"), "\\static \\function a => B.C.E.e");
    sourceSupplier.add(moduleName("B"), "\\static \\function b => 0");
    sourceSupplier.add(moduleName("B", "C"), null);
    sourceSupplier.add(moduleName("B", "C", "D"), "\\static \\function d => 0");
    sourceSupplier.add(moduleName("B", "C", "E"), "\\static \\function e => F.f");
    sourceSupplier.add(moduleName("B", "C", "F"), "\\static \\function f => 0");
    sourceSupplier.add(moduleName("All"), "\\export A \\export B \\export B.C \\export B.C.E \\export B.C.D \\export B.C.F");
    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.setOutputSupplier(outputSupplier);

    moduleLoader.load(new ResolvedName(RootModule.ROOT, "All"), false);

    TypecheckingOrdering.typecheck(loadedModules, errorReporter);

    for (ResolvedName module : loadedModules) {
      if (module.toAbstractDefinition() != null && module.toDefinition() != null) {
        moduleLoader.save(module);
      }
    }
    assertTrue(errorReporter.getErrorList().isEmpty());

    initializeModuleLoader();
    moduleLoader.setOutputSupplier(outputSupplier);
    moduleLoader.setSourceSupplier(sourceSupplier);
  }

  private void initializeModuleLoader() {
    RootModule.initialize();
    loadedModules = new ArrayList<>();
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
      public void loadingSucceeded(ResolvedName resolvedName, NamespaceMember namespaceMember, boolean compiled) {
        loadedModules.add(resolvedName);
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
    sourceSupplier.add(moduleName("A"), "\\static \\function f => B.g");
    sourceSupplier.add(moduleName("B"), "\\static \\function g => A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(new ResolvedName(RootModule.ROOT, "A"), false);
    assertFalse(errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestError2() {
    sourceSupplier.add(moduleName("A"), "\\static \\function f => B.g");
    sourceSupplier.add(moduleName("B"), "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(new ResolvedName(RootModule.ROOT, "A"), false);
    assertFalse(errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestError3() {
    sourceSupplier.add(moduleName("A"), "\\static \\function f => B.g \\static \\function h => 0");
    sourceSupplier.add(moduleName("B"), "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(new ResolvedName(RootModule.ROOT, "A"), false);
    assertFalse(errorReporter.getErrorList().isEmpty());
  }

  @Test
  public void nonStaticTestError() {
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat \\function h => f");
    sourceSupplier.add(moduleName("B"), "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    ModuleLoadingResult result = moduleLoader.load(new ResolvedName(RootModule.ROOT, "B"), false);
    TypecheckingOrdering.typecheck(result.namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void staticAbstractTestError() {
    sourceSupplier.add(moduleName("A"), "\\static \\abstract f : Nat");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(new ResolvedName(RootModule.ROOT, "A"), false).namespaceMember.getResolvedName();
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void moduleTest() {
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat \\static \\class C { \\abstract g : Nat \\function h => g }");
    moduleLoader.setSourceSupplier(sourceSupplier);

    ModuleLoadingResult result = moduleLoader.load(new ResolvedName(RootModule.ROOT, "A"), false);
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
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat \\static \\class B { \\abstract g : Nat \\function h => g }");
    sourceSupplier.add(moduleName("B"), "\\static \\function f (p : A.B) => p.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(new ResolvedName(RootModule.ROOT, "A"), false).namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 0, errorReporter.getErrorList().size());
  }

  @Test
  public void nonStaticTestError2() {
    sourceSupplier.add(moduleName("A"), "\\abstract f : Nat \\class B { \\abstract g : Nat \\static \\function (+) (f g : Nat) => f \\function h => f + g }");
    sourceSupplier.add(moduleName("B"), "\\static \\function f (p : A.B) => p.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(new ResolvedName(RootModule.ROOT, "B"), false).namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void abstractNonStaticTestError() {
    sourceSupplier.add(moduleName("A"), "\\function f : Nat");
    sourceSupplier.add(moduleName("B"), "\\function g => A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    TypecheckingOrdering.typecheck(moduleLoader.load(new ResolvedName(RootModule.ROOT, "B"), false).namespaceMember.getResolvedName(), errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), 1, errorReporter.getErrorList().size());
  }

  @Test
  public void testForTestCase() {
    setupSources();
    moduleLoader.load(new ResolvedName(RootModule.ROOT, "All"), false);
    assertTrue(errorReporter.getErrorList().isEmpty());
    assertTrue(RootModule.ROOT.findChild("B").getMember("C").abstractDefinition == null);
    assertTrue(RootModule.ROOT.getMember("All").abstractDefinition == null);
    assertTrue(RootModule.ROOT.getMember("A").abstractDefinition == null);
    assertTrue(RootModule.ROOT.getMember("B").abstractDefinition == null);
    assertTrue(RootModule.ROOT.findChild("B").findChild("C").getMember("D").abstractDefinition == null);
    assertTrue(RootModule.ROOT.findChild("B").findChild("C").getMember("E").abstractDefinition == null);
   }

  @Test
  public void testChange1() {
    setupSources();

    sourceSupplier.touch(moduleName("B"));
    moduleLoader.load(new ResolvedName(RootModule.ROOT, "All"), false);
    assertTrue(errorReporter.getErrorList().isEmpty());
    assertTrue(RootModule.ROOT.getMember("All").abstractDefinition != null);
    assertTrue(RootModule.ROOT.getMember("A").abstractDefinition != null);
    assertTrue(RootModule.ROOT.getMember("B").abstractDefinition != null);
    assertTrue(RootModule.ROOT.findChild("B").findChild("C").getMember("D").abstractDefinition == null);
    assertTrue(RootModule.ROOT.findChild("B").findChild("C").getMember("E").abstractDefinition != null);
    assertTrue(RootModule.ROOT.findChild("B").findChild("C").getMember("F").abstractDefinition == null);
  }

  @Test
  public void testChange2() {
    setupSources();

    sourceSupplier.touch(moduleName("B", "C", "D"));
    moduleLoader.load(new ResolvedName(RootModule.ROOT, "All"), false);
    assertTrue(errorReporter.getErrorList().isEmpty());
    assertTrue(RootModule.ROOT.getMember("All").abstractDefinition != null);
    assertTrue(RootModule.ROOT.getMember("A").abstractDefinition == null);
    assertTrue(RootModule.ROOT.getMember("B").abstractDefinition == null);
    assertTrue(RootModule.ROOT.findChild("B").findChild("C").getMember("D").abstractDefinition != null);
    assertTrue(RootModule.ROOT.findChild("B").findChild("C").getMember("E").abstractDefinition == null);
    assertTrue(RootModule.ROOT.findChild("B").findChild("C").getMember("F").abstractDefinition == null);
  }
}
