package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.frontend.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
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
    Concrete.ClassDefinition moduleB = (Concrete.ClassDefinition) moduleLoader.load(storage.locateModule(moduleName("B")));

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Abstract.TermFunctionBody) ((Abstract.FunctionDefinition) get(moduleB, "b")).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "x")));
  }

  @Test
  public void trivialResolutionThatLoads() {
    setupSources();
    Concrete.ClassDefinition moduleA = (Concrete.ClassDefinition) moduleLoader.load(storage.locateModule(moduleName("A")));

    ModuleNamespace moduleBNs = nameResolver.resolveModuleNamespace(moduleName("B"));
    assertThat(moduleBNs, is(notNullValue()));

    Concrete.ClassDefinition moduleB = (Concrete.ClassDefinition) moduleBNs.getRegisteredClass();
    assertThat(moduleB, is(notNullValue()));

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Abstract.TermFunctionBody) ((Abstract.FunctionDefinition) get(moduleA, "a")).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "b")));
  }

  @Test
  public void resolutionThatLoadsMultipleModules() {
    setupSources();
    Concrete.ClassDefinition moduleBC = (Concrete.ClassDefinition) moduleLoader.load(storage.locateModule(moduleName("B", "C")));
    Concrete.ClassDefinition moduleBCE = (Concrete.ClassDefinition) nameResolver.resolveModuleNamespace(moduleName("B", "C", "E")).getRegisteredClass();
    Concrete.ClassDefinition moduleBCF = (Concrete.ClassDefinition) nameResolver.resolveModuleNamespace(moduleName("B", "C", "F")).getRegisteredClass();

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Abstract.TermFunctionBody) ((Abstract.FunctionDefinition) get(moduleBC, "c")).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleBCE, "e")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Abstract.TermFunctionBody) ((Abstract.FunctionDefinition) get(moduleBCE, "e")).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleBCF, "f")));
  }

  @Test
  public void mutuallyRecursiveModules() {
    setupSources();
    Concrete.ClassDefinition moduleX = (Concrete.ClassDefinition) moduleLoader.load(storage.locateModule(moduleName("X")));
    GlobalReferable moduleY = nameResolver.resolveModuleNamespace(moduleName("Y")).getRegisteredClass();

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Abstract.TermFunctionBody) ((Abstract.FunctionDefinition) get(moduleX, "f")).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleY, "f")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Abstract.TermFunctionBody) ((Abstract.FunctionDefinition) get(moduleY, "f")).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleX, "f")));
  }

  @Test
  public void duplicateNamesOnTopLevel() {
    storage.add(moduleName("Test"), "\\function a => 0 \n \\function a => 0");
    moduleLoader.load(storage.locateModule(moduleName("Test")));
  }
}
