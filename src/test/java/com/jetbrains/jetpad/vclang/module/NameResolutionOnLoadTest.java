package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.SimpleModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.SourceModuleLoader;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import org.junit.Before;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.module.ModulePath.moduleName;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class NameResolutionOnLoadTest extends NameResolverTestCase {
  private MemoryStorage storage;
  private SourceModuleLoader<MemoryStorage.SourceId> moduleLoader;

  @Before
  public void initialize() {
    storage = new MemoryStorage(moduleNsProvider, nameResolver);
    moduleLoader = new SimpleModuleLoader<>(storage, errorReporter);
    nameResolver.setModuleResolver(storage, errorReporter);
  }

  private void setupSources() {
    storage.add(moduleName("A"), "\\function a => ::B.b");
    storage.add(moduleName("B"), "\\function b => x\n \\function x => 0");
    storage.add(moduleName("B", "C"), "\\function c => ::B::C::E.e");
    storage.add(moduleName("B", "C", "E"), "\\function e => ::B::C::F.f");
    storage.add(moduleName("B", "C", "F"), "\\function f => 0");
    storage.add(moduleName("X"), "\\function f => ::Y.f");
    storage.add(moduleName("Y"), "\\function f => ::X.f");
  }

  @Test
  public void trivialResolution() {
    setupSources();
    Abstract.ClassDefinition moduleB = moduleLoader.load(storage.locateModule(moduleName("B")));

    Abstract.DefCallExpression defCall = (Abstract.DefCallExpression) ((Abstract.FunctionDefinition) get(moduleB, "b")).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "x")));
  }

  @Test
  public void trivialResolutionThatLoads() {
    setupSources();
    Abstract.ClassDefinition moduleA = moduleLoader.load(storage.locateModule(moduleName("A")));

    ModuleNamespace moduleBNs = nameResolver.resolveModuleNamespace(moduleName("B"));
    assertThat(moduleBNs, is(notNullValue()));

    Abstract.ClassDefinition moduleB = moduleBNs.getRegisteredClass();
    assertThat(moduleB, is(notNullValue()));

    Abstract.DefCallExpression defCall = (Abstract.DefCallExpression) ((Abstract.FunctionDefinition) get(moduleA, "a")).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "b")));
  }

  @Test
  public void resolutionThatLoadsMultipleModules() {
    setupSources();
    Abstract.ClassDefinition moduleBC = moduleLoader.load(storage.locateModule(moduleName("B", "C")));
    Abstract.ClassDefinition moduleBCE = nameResolver.resolveModuleNamespace(moduleName("B", "C", "E")).getRegisteredClass();
    Abstract.ClassDefinition moduleBCF = nameResolver.resolveModuleNamespace(moduleName("B", "C", "F")).getRegisteredClass();

    Abstract.DefCallExpression defCall1 = (Abstract.DefCallExpression) ((Abstract.FunctionDefinition) get(moduleBC, "c")).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleBCE, "e")));

    Abstract.DefCallExpression defCall2 = (Abstract.DefCallExpression) ((Abstract.FunctionDefinition) get(moduleBCE, "e")).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleBCF, "f")));
  }

  @Test
  public void mutuallyRecursiveModules() {
    setupSources();
    Abstract.ClassDefinition moduleX = moduleLoader.load(storage.locateModule(moduleName("X")));
    Abstract.ClassDefinition moduleY = nameResolver.resolveModuleNamespace(moduleName("Y")).getRegisteredClass();

    Abstract.DefCallExpression defCall1 = (Abstract.DefCallExpression) ((Abstract.FunctionDefinition) get(moduleX, "f")).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleY, "f")));

    Abstract.DefCallExpression defCall2 = (Abstract.DefCallExpression) ((Abstract.FunctionDefinition) get(moduleY, "f")).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleX, "f")));
  }

  @Test
  public void duplicateNamesOnTopLevel() {
    storage.add(moduleName("Test"), "\\function a => 0 \n \\function a => 0");
    moduleLoader.load(storage.locateModule(moduleName("Test")));
  }
}
