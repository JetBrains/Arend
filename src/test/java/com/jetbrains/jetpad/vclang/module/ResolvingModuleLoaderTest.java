package com.jetbrains.jetpad.vclang.module;

public class ResolvingModuleLoaderTest {
  /*
  private void assertLoaded(String... path) {
    ModuleID moduleID = moduleLoader.locateModule(moduleName(path));
    assertTrue(loadedCompiledModules.contains(moduleID));
  }

  private void assertNotLoaded(String... path) {
    ModuleID moduleID = moduleLoader.locateModule(moduleName(path));
    assertTrue(loadedModules.contains(moduleID) && !loadedCompiledModules.contains(moduleID));
  }

  private void setupSources() {
    storage.add(moduleName("A"), "\\static \\function a => ::B::C::E.e");
    storage.add(moduleName("B"), "\\static \\function b => 0");
    storage.add(moduleName("B", "C", "D"), "\\static \\function d => 0");
    storage.add(moduleName("B", "C", "E"), "\\static \\function e => ::B::C::F.f");
    storage.add(moduleName("B", "C", "F"), "\\static \\function f => 0");
    storage.add(moduleName("All"), "\\export ::A \\export ::B \\export ::B::C::E \\export ::B::C::D \\export ::B::C::F");

    moduleLoader.load(moduleName("All"));

    TypecheckingOrdering.typecheck(loadedModuleRoots, errorReporter);

    for (ModuleID module : loadedModules) {
      moduleLoader.save(module);
    }
    assertErrorListIsEmpty(errorReporter.getErrorList());

    initializeModuleLoader();
    moduleLoader.setOutputSupplier(outputSupplier);
    moduleLoader.setSourceSupplier(storage);
  }

  @Test
  public void testForTestCase() {
    setupSources();

    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertErrorListIsEmpty(errorReporter.getErrorList());
    assertLoaded("All");
    assertLoaded("A");
    assertLoaded("B");
    assertLoaded("B", "C", "E");
    assertLoaded("B", "C", "D");
   }

  @Test
  public void testChange1() {
    setupSources();

    storage.touch(moduleName("B"));
    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertErrorListIsEmpty(errorReporter.getErrorList());
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

    storage.touch(moduleName("B", "C", "D"));
    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertErrorListIsEmpty(errorReporter.getErrorList());
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

    storage.add(moduleName("B", "C", "G"), "\\static \\function G => f");
    moduleLoader.load(moduleLoader.locateModule(moduleName("B")));
    moduleLoader.load(moduleLoader.locateModule(moduleName("B", "C", "G")));
    moduleLoader.load(moduleLoader.locateModule(moduleName("All")));
    assertErrorListIsEmpty(errorReporter.getErrorList());
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
    assertErrorListIsEmpty(errorReporter.getErrorList());
    assertNotLoaded("All");
    assertLoaded("A");
    assertLoaded("B", "C", "D");
    assertLoaded("B", "C", "E");
    assertLoaded("B", "C", "F");
  }

  @Test
  public void testUnknownNameReport() {
    storage.add(moduleName("A"), "\\static \\function f => 0");
    storage.add(moduleName("B"), "\\static \\function g => ::A.asdfas");

    moduleLoader.setSourceSupplier(storage);
    TypecheckingOrdering.typecheck(moduleLoader.load(moduleLoader.locateModule(moduleName("B"))).abstractDefinition, errorReporter);
    assertErrorListNotEmpty(errorReporter.getErrorList());
  }
  */
}
