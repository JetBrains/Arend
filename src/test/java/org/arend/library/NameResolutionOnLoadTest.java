package org.arend.library;

import org.arend.ext.module.ModulePath;
import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.naming.scope.Scope;
import org.arend.source.SourceLoader;
import org.arend.term.concrete.Concrete;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class NameResolutionOnLoadTest extends LibraryTestCase {
  private void setupSources() {
    library.addModule(new ModulePath("A"), "\\import B() \\func a => B.b");
    library.addModule(new ModulePath("B"), "\\func b => x\n \\func x => 0");
    library.addModule(new ModulePath("B", "C"), "\\import B.C.E() \\func c => B.C.E.e");
    library.addModule(new ModulePath("B", "C", "E"), "\\import B.C.F() \\func e => B.C.F.f");
    library.addModule(new ModulePath("B", "C", "F"), "\\func f => 0");
    library.addModule(new ModulePath("X"), "\\import Y() \\func f => Y.f");
    library.addModule(new ModulePath("Y"), "\\import X() \\func f => X.f");
  }

  @Test
  public void trivialResolution() {
    setupSources();
    SourceLoader sourceLoader = new SourceLoader(library, libraryManager);
    assertTrue(sourceLoader.preloadRaw(new ModulePath("B")));
    sourceLoader.loadRawSources();
    Scope moduleB = library.getModuleScopeProvider().forModule(new ModulePath("B"));

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleB, "b")).getDefinition()).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "x")));
  }

  @Test
  public void trivialResolutionThatLoads() {
    setupSources();
    SourceLoader sourceLoader = new SourceLoader(library, libraryManager);
    assertTrue(sourceLoader.preloadRaw(new ModulePath("A")));
    sourceLoader.loadRawSources();
    Scope moduleA = library.getModuleScopeProvider().forModule(new ModulePath("A"));

    Scope moduleB = library.getModuleScopeProvider().forModule(new ModulePath("B"));
    assertThat(moduleB, is(notNullValue()));

    Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleA, "a")).getDefinition()).getBody()).getTerm();

    assertThat(defCall.getReferent(), is(notNullValue()));
    assertThat(defCall.getReferent(), is(get(moduleB, "b")));
  }

  @Test
  public void resolutionThatLoadsMultipleModules() {
    setupSources();
    SourceLoader sourceLoader = new SourceLoader(library, libraryManager);
    assertTrue(sourceLoader.preloadRaw(new ModulePath("B", "C")));
    sourceLoader.loadRawSources();
    Scope moduleBC = library.getModuleScopeProvider().forModule(new ModulePath("B", "C"));
    Scope moduleBCE = library.getModuleScopeProvider().forModule(new ModulePath("B", "C", "E"));
    Scope moduleBCF = library.getModuleScopeProvider().forModule(new ModulePath("B", "C", "F"));

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
    SourceLoader sourceLoader = new SourceLoader(library, libraryManager);
    assertTrue(sourceLoader.preloadRaw(new ModulePath("X")));
    sourceLoader.loadRawSources();
    Scope moduleX = library.getModuleScopeProvider().forModule(new ModulePath("X"));
    Scope moduleY = library.getModuleScopeProvider().forModule(new ModulePath("Y"));

    Concrete.ReferenceExpression defCall1 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleX, "f")).getDefinition()).getBody()).getTerm();
    assertThat(defCall1.getReferent(), is(notNullValue()));
    assertThat(defCall1.getReferent(), is(get(moduleY, "f")));

    Concrete.ReferenceExpression defCall2 = (Concrete.ReferenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((ConcreteLocatedReferable) get(moduleY, "f")).getDefinition()).getBody()).getTerm();
    assertThat(defCall2.getReferent(), is(notNullValue()));
    assertThat(defCall2.getReferent(), is(get(moduleX, "f")));
  }

  @Test
  public void duplicateNamesOnTopLevel() {
    library.addModule(new ModulePath("Test"), "\\func a => 0 \n \\func a => 0");
    libraryManager.loadLibrary(library, null);
  }
}
