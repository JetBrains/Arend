package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteLocatedReferable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.source.SourceLoader;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.module.ModulePath.moduleName;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class NameResolutionOnLoadTest extends LibraryTestCase {
  private void setupSources() {
    library.addModule(moduleName("A"), "\\import B() \\func a => B.b");
    library.addModule(moduleName("B"), "\\func b => x\n \\func x => 0");
    library.addModule(moduleName("B", "C"), "\\import B.C.E() \\func c => B.C.E.e");
    library.addModule(moduleName("B", "C", "E"), "\\import B.C.F() \\func e => B.C.F.f");
    library.addModule(moduleName("B", "C", "F"), "\\func f => 0");
    library.addModule(moduleName("X"), "\\import Y() \\func f => Y.f");
    library.addModule(moduleName("Y"), "\\import X() \\func f => X.f");
  }

  @Test
  public void trivialResolution() {
    setupSources();
    Source source = library.getRawSource(moduleName("B"));
    assertThat(source, is(notNullValue()));
    assertTrue(source.load(new SourceLoader(library, libraryManager, false)));
    Scope moduleB = library.getModuleScopeProvider().forModule(moduleName("B"));

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleB, "b")).getDefinition()).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "x")));
  }

  @Test
  public void trivialResolutionThatLoads() {
    setupSources();
    Source source = library.getRawSource(moduleName("A"));
    assertThat(source, is(notNullValue()));
    assertTrue(source.load(new SourceLoader(library, libraryManager, false)));
    Scope moduleA = library.getModuleScopeProvider().forModule(moduleName("A"));

    Scope moduleB = library.getModuleScopeProvider().forModule(moduleName("B"));
    assertThat(moduleB, is(notNullValue()));

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleA, "a")).getDefinition()).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "b")));
  }

  @Test
  public void resolutionThatLoadsMultipleModules() {
    setupSources();
    Source source = library.getRawSource(moduleName("B", "C"));
    assertThat(source, is(notNullValue()));
    assertTrue(source.load(new SourceLoader(library, libraryManager, false)));
    Scope moduleBC = library.getModuleScopeProvider().forModule(moduleName("B", "C"));
    Scope moduleBCE = library.getModuleScopeProvider().forModule(moduleName("B", "C", "E"));
    Scope moduleBCF = library.getModuleScopeProvider().forModule(moduleName("B", "C", "F"));

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleBC, "c")).getDefinition()).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleBCE, "e")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleBCE, "e")).getDefinition()).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleBCF, "f")));
  }

  @Test
  public void mutuallyRecursiveModules() {
    setupSources();
    Source source = library.getRawSource(moduleName("X"));
    assertThat(source, is(notNullValue()));
    assertTrue(source.load(new SourceLoader(library, libraryManager, false)));
    Scope moduleX = library.getModuleScopeProvider().forModule(moduleName("X"));
    Scope moduleY = library.getModuleScopeProvider().forModule(moduleName("Y"));

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleX, "f")).getDefinition()).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleY, "f")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleY, "f")).getDefinition()).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleX, "f")));
  }

  @Test
  public void duplicateNamesOnTopLevel() {
    library.addModule(moduleName("Test"), "\\func a => 0 \n \\func a => 0");
    libraryManager.loadLibrary(library);
  }
}
