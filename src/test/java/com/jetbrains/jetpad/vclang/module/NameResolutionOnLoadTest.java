package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.frontend.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import org.junit.Before;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.module.ModulePath.moduleName;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class NameResolutionOnLoadTest extends NameResolverTestCase {
  private MemoryStorage storage;
  private BaseModuleLoader<MemoryStorage.SourceId> moduleLoader;

  @Before
  public void initialize() {
    storage = new MemoryStorage(moduleNsProvider, nameResolver);
    moduleLoader = new BaseModuleLoader<>(storage, errorReporter);
    nameResolver.setModuleResolver(moduleLoader);
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
    GlobalReferable moduleB = moduleLoader.load(storage.locateModule(moduleName("B"))).getReferable();

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((GlobalReference) get(moduleB, "b")).getDefinition()).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "x")));
  }

  @Test
  public void trivialResolutionThatLoads() {
    setupSources();
    GlobalReferable moduleA = moduleLoader.load(storage.locateModule(moduleName("A"))).getReferable();

    ModuleNamespace moduleBNs = nameResolver.resolveModuleNamespace(moduleName("B"));
    assertThat(moduleBNs, is(notNullValue()));

    GlobalReferable moduleB = moduleBNs.getRegisteredClass();
    assertThat(moduleB, is(notNullValue()));

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((GlobalReference) get(moduleA, "a")).getDefinition()).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "b")));
  }

  @Test
  public void resolutionThatLoadsMultipleModules() {
    setupSources();
    GlobalReferable moduleBC = moduleLoader.load(storage.locateModule(moduleName("B", "C"))).getReferable();
    GlobalReferable moduleBCE = nameResolver.resolveModuleNamespace(moduleName("B", "C", "E")).getRegisteredClass();
    GlobalReferable moduleBCF = nameResolver.resolveModuleNamespace(moduleName("B", "C", "F")).getRegisteredClass();

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((GlobalReference) get(moduleBC, "c")).getDefinition()).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleBCE, "e")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((GlobalReference) get(moduleBCE, "e")).getDefinition()).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleBCF, "f")));
  }

  @Test
  public void mutuallyRecursiveModules() {
    setupSources();
    GlobalReferable moduleX = moduleLoader.load(storage.locateModule(moduleName("X"))).getReferable();
    GlobalReferable moduleY = nameResolver.resolveModuleNamespace(moduleName("Y")).getRegisteredClass();

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((GlobalReference) get(moduleX, "f")).getDefinition()).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleY, "f")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((GlobalReference) get(moduleY, "f")).getDefinition()).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleX, "f")));
  }

  @Test
  public void duplicateNamesOnTopLevel() {
    storage.add(moduleName("Test"), "\\function a => 0 \n \\function a => 0");
    moduleLoader.load(storage.locateModule(moduleName("Test")));
  }
}
