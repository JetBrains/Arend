package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.frontend.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
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
    storage = new MemoryStorage(moduleScopeProvider, null, moduleScopeProvider);
    moduleLoader = new BaseModuleLoader<>(storage, errorReporter);
    storage.setModuleResolver(moduleLoader);
  }

  private void setupSources() {
    storage.add(moduleName("A"), "\\import B() \\function a => B.b");
    storage.add(moduleName("B"), "\\function b => x\n \\function x => 0");
    storage.add(moduleName("B", "C"), "\\import B.C.E() \\function c => B.C.E.e");
    storage.add(moduleName("B", "C", "E"), "\\import B.C.F() \\function e => B.C.F.f");
    storage.add(moduleName("B", "C", "F"), "\\function f => 0");
    storage.add(moduleName("X"), "\\import Y() \\function f => Y.f");
    storage.add(moduleName("Y"), "\\import X() \\function f => X.f");
  }

  @Test
  public void trivialResolution() {
    setupSources();
    Scope moduleB = moduleLoader.load(storage.locateModule(moduleName("B"))).getGroupScope();

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteGlobalReferable) get(moduleB, "b")).getDefinition()).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "x")));
  }

  @Test
  public void trivialResolutionThatLoads() {
    setupSources();
    Scope moduleA = moduleLoader.load(storage.locateModule(moduleName("A"))).getGroupScope();

    Scope moduleB = moduleScopeProvider.forModule(moduleName("B"));
    assertThat(moduleB, is(notNullValue()));

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteGlobalReferable) get(moduleA, "a")).getDefinition()).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "b")));
  }

  @Test
  public void resolutionThatLoadsMultipleModules() {
    setupSources();
    Scope moduleBC = moduleLoader.load(storage.locateModule(moduleName("B", "C"))).getGroupScope();
    Scope moduleBCE = moduleScopeProvider.forModule(moduleName("B", "C", "E"));
    Scope moduleBCF = moduleScopeProvider.forModule(moduleName("B", "C", "F"));

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteGlobalReferable) get(moduleBC, "c")).getDefinition()).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleBCE, "e")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteGlobalReferable) get(moduleBCE, "e")).getDefinition()).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleBCF, "f")));
  }

  @Test
  public void mutuallyRecursiveModules() {
    setupSources();
    Scope moduleX = moduleLoader.load(storage.locateModule(moduleName("X"))).getGroupScope();
    Scope moduleY = moduleScopeProvider.forModule(moduleName("Y"));

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteGlobalReferable) get(moduleX, "f")).getDefinition()).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleY, "f")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteGlobalReferable) get(moduleY, "f")).getDefinition()).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleX, "f")));
  }

  @Test
  public void duplicateNamesOnTopLevel() {
    storage.add(moduleName("Test"), "\\function a => 0 \n \\function a => 0");
    moduleLoader.load(storage.locateModule(moduleName("Test")));
  }
}
