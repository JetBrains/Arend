package org.arend.library;

import org.arend.core.definition.Definition;
import org.arend.ext.module.ModulePath;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.source.Source;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.arend.Matchers.goal;
import static org.arend.Matchers.typecheckingError;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CachingTest extends LibraryTestCase {
  @Test
  public void statusSerialization() {
    library.addModule(new ModulePath("A"),
        "\\func a : \\Set0 => \\Prop\n" +
        "\\func b1 : \\Set0 => \\Set0\n" +
        "\\func b2 : \\Set0 => b1");
    assertTrue(libraryManager.loadLibrary(library, null));
    ChildGroup aClass = library.getModuleGroup(new ModulePath("A"));
    assertThat(aClass, is(notNullValue()));

    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    assertThat(errorList, hasSize(1));
    errorList.clear();

    Definition.TypeCheckingStatus aStatus = getDef(aClass.getGroupScope(), "a").getTypechecked().status();

    libraryManager.unloadLibrary(library);

    assertTrue(libraryManager.loadLibrary(library, null));
    aClass = library.getModuleGroup(new ModulePath("A"));
    assertThat(aClass, is(notNullValue()));

    assertThat(getDef(aClass.getGroupScope(), "a").getTypechecked().status(), is(equalTo(aStatus)));
    assertThat(getDef(aClass.getGroupScope(), "b1").getTypechecked(), is(nullValue()));
    assertThat(getDef(aClass.getGroupScope(), "b2").getTypechecked(), is(nullValue()));
  }

  @Test
  public void circularDependencies() {
    library.addModule(new ModulePath("A"), "\\import B() \\func a (n : Nat) : Nat | zero => zero | suc n => B.b n");
    library.addModule(new ModulePath("B"), "\\import A() \\func b (n : Nat) : Nat | zero => zero | suc n => A.a n");
    libraryManager.loadLibrary(library, null);
    assertThat(library.getModuleGroup(new ModulePath("A")), is(notNullValue()));
    assertThat(library.getModuleGroup(new ModulePath("B")), is(notNullValue()));
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    assertThat(errorList, is(empty()));
  }

  @Test
  public void errorInBody() {
    library.addModule(new ModulePath("A"),
        "\\func a : \\Set0 => b\n" +
        "\\func b : \\Set0 => {?}");
    libraryManager.loadLibrary(library, null);
    ChildGroup aGroup = library.getModuleGroup(new ModulePath("A"));
    assertThat(aGroup, is(notNullValue()));
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    assertThatErrorsAre(goal(0));
    errorList.clear();

    libraryManager.unloadLibrary(library);
    assertThat(getDef(aGroup.getGroupScope(), "a").getTypechecked(), is(nullValue()));
    assertThat(getDef(aGroup.getGroupScope(), "b").getTypechecked(), is(nullValue()));

    libraryManager.loadLibrary(library, null);
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    ChildGroup aGroup2 = library.getModuleGroup(new ModulePath("A"));
    assertThat(aGroup2, is(notNullValue()));
    assertThat(getDef(aGroup2.getGroupScope(), "a").getTypechecked(), is(notNullValue()));
    assertThat(getDef(aGroup2.getGroupScope(), "b").getTypechecked(), is(notNullValue()));
  }

  @Test
  public void errorInHeader() {
    library.addModule(new ModulePath("A"),
        "\\data D\n" +
        "\\func a (d : D) \\with\n" +
        "\\func b : \\Set0 => (\\lam x y => x) \\Prop a");
    libraryManager.loadLibrary(library, null);
    ChildGroup aGroup = library.getModuleGroup(new ModulePath("A"));
    assertThat(aGroup, is(notNullValue()));

    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    assertThatErrorsAre(typecheckingError());
    errorList.clear();

    libraryManager.unloadLibrary(library);
    assertThat(getDef(aGroup.getGroupScope(), "D").getTypechecked(), is(nullValue()));
    assertThat(getDef(aGroup.getGroupScope(), "a").getTypechecked(), is(nullValue()));
    assertThat(getDef(aGroup.getGroupScope(), "b").getTypechecked(), is(nullValue()));

    libraryManager.loadLibrary(library, null);
    aGroup = library.getModuleGroup(new ModulePath("A"));
    assertThat(aGroup, is(notNullValue()));

    assertThat(getDef(aGroup.getGroupScope(), "D").getTypechecked(), is(notNullValue()));
    assertThat(getDef(aGroup.getGroupScope(), "a").getTypechecked(), is(nullValue()));
    assertThat(getDef(aGroup.getGroupScope(), "b").getTypechecked(), is(nullValue()));
  }

  @Test
  public void sourceDoesNotChanged() {
    library.addModule(new ModulePath("A"), "\\data D\n");
    libraryManager.loadLibrary(library, null);
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    libraryManager.unloadLibrary(library);

    library.updateModule(new ModulePath("A"), "\\func f => 0", false);
    libraryManager.loadLibrary(library, null);
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    assertThat(getDef(library.getModuleScopeProvider().forModule(new ModulePath("a")), "f"), is(nullValue()));
    assertThat(getDef(library.getModuleScopeProvider().forModule(new ModulePath("A")), "D"), is(nullValue()));
  }

  @Test
  public void sourceChanged() {
    library.addModule(new ModulePath("A"), "\\data D\n");
    libraryManager.loadLibrary(library, null);
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    libraryManager.unloadLibrary(library);

    library.updateModule(new ModulePath("A"), "\\func f => 0", true);
    libraryManager.loadLibrary(library, null);
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    assertThat(getDef(library.getModuleScopeProvider().forModule(new ModulePath("a")), "D"), is(nullValue()));
    assertThat(getDef(library.getModuleScopeProvider().forModule(new ModulePath("A")), "f").getTypechecked(), is(notNullValue()));
  }

  @Test
  public void dependencySourceChanged() {
    library.addModule(new ModulePath("A"), "\\data D\n");
    library.addModule(new ModulePath("B"), "\\import A() \\func f : \\Type0 => A.D\n");
    libraryManager.loadLibrary(library, null);
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    libraryManager.unloadLibrary(library);

    library.updateModule(new ModulePath("A"), "\\data D'", true);
    libraryManager.loadLibrary(library, null);
    assertThat(errorList, is(not(empty())));
    errorList.clear();
    typechecking.typecheckLibrary(library);
    library.persistUpdatedModules(errorReporter);
    assertThat(errorList, is(empty()));
  }

  /* These tests does not make sense with the current implementation of libraries.
  @Test
  public void removeRawSource() {
    library.addModule(moduleName("A"), "\\func a : \\1-Type1 => \\Set0");
    library.addModule(moduleName("B"),
      "\\import A\n" +
      "\\func b : \\1-Type1 => A.a");
    libraryManager.loadLibrary(library);
    library.typecheck(typechecking);
    library.persistUpdateModules(errorReporter);
    libraryManager.unloadLibrary(library);

    library.removeRawSource(moduleName("A"));

    assertTrue(libraryManager.loadLibrary(library));
    assertThat(errorList, is(empty()));
  }

  @Test
  public void removeBothSource() {
    library.addModule(moduleName("A"), "\\func a : \\1-Type1 => \\Set0");
    library.addModule(moduleName("B"),
      "\\import A\n" +
        "\\func b : \\1-Type1 => A.a");
    libraryManager.loadLibrary(library);
    library.typecheck(typechecking);
    library.persistUpdateModules(errorReporter);
    libraryManager.unloadLibrary(library);

    library.removeRawSource(moduleName("A"));
    library.removeBinarySource(moduleName("A"));

    assertFalse(libraryManager.loadLibrary(library));
    assertThat(errorList, is(not(empty())));
  }

  @Test
  public void removeSourceWithError() {
    library.addModule(moduleName("A"), "\\func a : \\Set0 => \\Set0");  // There is an error here
    library.addModule(moduleName("B"),
      "\\import A\n" +
      "\\func b : \\1-Type1 => A.a");
    libraryManager.loadLibrary(library);
    library.typecheck(typechecking);
    library.persistUpdateModules(errorReporter);
    assertThat(errorList, hasSize(2));
    errorList.clear();

    libraryManager.unloadLibrary(library);
    library.removeRawSource(moduleName("A"));
    library.removeBinarySource(moduleName("B"));

    libraryManager.loadLibrary(library);
    library.typecheck(typechecking);
    library.persistUpdateModules(errorReporter);

    Scope aScope = library.getModuleScopeProvider().forModule(moduleName("A"));
    assertThat(errorList, hasSize(1));
    assertThatErrorsAre(hasErrors(getDef(aScope, "a")));
  }

  @Test
  public void removeSourceErrorInReferrer() {
    library.addModule(moduleName("A"),
      "\\func a : \\1-Type1 => \\Set0");
    library.addModule(moduleName("B"),
      "\\import A\n" +
      "\\func b : \\Set0 => A.a"); // There is an error here
    libraryManager.loadLibrary(library);
    library.typecheck(typechecking);
    library.persistUpdateModules(errorReporter);
    assertThat(errorList, hasSize(1));
    errorList.clear();

    libraryManager.unloadLibrary(library);
    library.removeRawSource(moduleName("A"));
    library.removeBinarySource(moduleName("B"));

    assertTrue(libraryManager.loadLibrary(library));
    library.typecheck(typechecking);
    library.persistUpdateModules(errorReporter);
    assertThat(errorList, hasSize(1));
    assertThatErrorsAre(typeMismatchError());
  }
  */

  @Test
  public void persistDependencyWhenReferrerHasErrorInHeader() {
    library.addModule(new ModulePath("A"), "\\func a : \\1-Type1 => \\Set0");
    library.addModule(new ModulePath("B"),
      "\\import A\n" +
      "\\func b : \\Set0 => A.a"); // There is an error here
    libraryManager.loadLibrary(library, null);
    List<Group> groups = new ArrayList<>(2);
    groups.add(library.getModuleGroup(new ModulePath("A")));
    groups.add(library.getModuleGroup(new ModulePath("B")));
    assertTrue(typechecking.typecheckModules(groups, null));
    assertThat(errorList, hasSize(1));
    errorList.clear();
    library.persistModule(new ModulePath("B"), IdReferableConverter.INSTANCE, errorReporter);
    assertThat(errorList, is(empty()));

    Source sourceA = library.getBinarySource(new ModulePath("A"));
    assertThat(sourceA, is(notNullValue()));
    assertFalse(sourceA.isAvailable());
    Source sourceB = library.getBinarySource(new ModulePath("B"));
    assertThat(sourceB, is(notNullValue()));
    assertTrue(sourceB.isAvailable());
  }
}
