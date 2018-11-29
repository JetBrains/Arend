package org.arend.library;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.source.Source;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.arend.module.ModulePath.moduleName;
import static org.arend.typechecking.Matchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class CachingTest extends LibraryTestCase {
  @Test
  public void statusSerialization() {
    library.addModule(moduleName("A"),
        "\\func a : \\Set0 => \\Prop\n" +
        "\\func b1 : \\Set0 => \\Set0\n" +
        "\\func b2 : \\Set0 => b1");
    assertTrue(libraryManager.loadLibrary(library));
    ChildGroup aClass = library.getModuleGroup(moduleName("A"));
    assertThat(aClass, is(notNullValue()));

    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    assertThat(errorList, hasSize(2));
    errorList.clear();

    Definition.TypeCheckingStatus aStatus = typecheckerState.getTypechecked(get(aClass.getGroupScope(), "a")).status();

    libraryManager.unloadLibrary(library);

    assertTrue(libraryManager.loadLibrary(library));
    aClass = library.getModuleGroup(moduleName("A"));
    assertThat(aClass, is(notNullValue()));

    assertThat(typecheckerState.getTypechecked(get(aClass.getGroupScope(), "a")).status(), is(equalTo(aStatus)));
    assertThat(typecheckerState.getTypechecked(get(aClass.getGroupScope(), "b1")).status(), is(equalTo(Definition.TypeCheckingStatus.MAY_BE_TYPE_CHECKED_WITH_ERRORS)));
    assertThat(typecheckerState.getTypechecked(get(aClass.getGroupScope(), "b2")).status(), is(equalTo(Definition.TypeCheckingStatus.MAY_BE_TYPE_CHECKED_WITH_WARNINGS)));
  }

  @Test
  public void circularDependencies() {
    library.addModule(moduleName("A"), "\\import B() \\func a (n : Nat) : Nat | zero => zero | suc n => B.b n");
    library.addModule(moduleName("B"), "\\import A() \\func b (n : Nat) : Nat | zero => zero | suc n => A.a n");
    libraryManager.loadLibrary(library);
    assertThat(library.getModuleGroup(moduleName("A")), is(notNullValue()));
    assertThat(library.getModuleGroup(moduleName("B")), is(notNullValue()));
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    assertThat(errorList, is(empty()));
  }

  @Test
  public void errorInBody() {
    library.addModule(moduleName("A"),
        "\\func a : \\Set0 => b\n" +
        "\\func b : \\Set0 => {?}");
    libraryManager.loadLibrary(library);
    ChildGroup aGroup = library.getModuleGroup(moduleName("A"));
    assertThat(aGroup, is(notNullValue()));
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    assertThatErrorsAre(goal(0));
    errorList.clear();

    libraryManager.unloadLibrary(library);
    assertThat(typecheckerState.getTypechecked(get(aGroup.getGroupScope(), "a")), is(nullValue()));
    assertThat(typecheckerState.getTypechecked(get(aGroup.getGroupScope(), "b")), is(nullValue()));

    libraryManager.loadLibrary(library);
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    ChildGroup aGroup2 = library.getModuleGroup(moduleName("A"));
    assertThat(aGroup2, is(notNullValue()));
    assertThat(typecheckerState.getTypechecked(get(aGroup2.getGroupScope(), "a")), is(notNullValue()));
    assertThat(typecheckerState.getTypechecked(get(aGroup2.getGroupScope(), "b")), is(notNullValue()));
  }

  @Test
  public void errorInHeader() {
    library.addModule(moduleName("A"),
        "\\data D\n" +
        "\\func a (d : D) \\with\n" +
        "\\func b : \\Set0 => (\\lam x y => x) \\Prop a");
    libraryManager.loadLibrary(library);
    ChildGroup aGroup = library.getModuleGroup(moduleName("A"));
    assertThat(aGroup, is(notNullValue()));

    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    assertThatErrorsAre(typecheckingError(), hasErrors(get(aGroup.getGroupScope(), "a")));
    errorList.clear();

    libraryManager.unloadLibrary(library);
    assertThat(typecheckerState.getTypechecked(get(aGroup.getGroupScope(), "D")), is(nullValue()));
    assertThat(typecheckerState.getTypechecked(get(aGroup.getGroupScope(), "a")), is(nullValue()));
    assertThat(typecheckerState.getTypechecked(get(aGroup.getGroupScope(), "b")), is(nullValue()));

    libraryManager.loadLibrary(library);
    aGroup = library.getModuleGroup(moduleName("A"));
    assertThat(aGroup, is(notNullValue()));

    assertThat(typecheckerState.getTypechecked(get(aGroup.getGroupScope(), "D")), is(notNullValue()));
    assertThat(typecheckerState.getTypechecked(get(aGroup.getGroupScope(), "a")), is(notNullValue()));
    assertThat(typecheckerState.getTypechecked(get(aGroup.getGroupScope(), "b")), is(notNullValue()));
  }

  @Test
  public void sourceDoesNotChanged() {
    library.addModule(moduleName("A"), "\\data D\n");
    libraryManager.loadLibrary(library);
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    libraryManager.unloadLibrary(library);

    library.updateModule(moduleName("A"), "\\func f => 0", false);
    libraryManager.loadLibrary(library);
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    assertThat(get(library.getModuleScopeProvider().forModule(moduleName("a")), "f"), is(nullValue()));
    assertThat(get(library.getModuleScopeProvider().forModule(moduleName("A")), "D"), is(nullValue()));
  }

  @Test
  public void sourceChanged() {
    library.addModule(moduleName("A"), "\\data D\n");
    libraryManager.loadLibrary(library);
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    libraryManager.unloadLibrary(library);

    library.updateModule(moduleName("A"), "\\func f => 0", true);
    libraryManager.loadLibrary(library);
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    assertThat(get(library.getModuleScopeProvider().forModule(moduleName("a")), "D"), is(nullValue()));
    assertThat(typecheckerState.getTypechecked(get(library.getModuleScopeProvider().forModule(moduleName("A")), "f")), is(notNullValue()));
  }

  @Test
  public void dependencySourceChanged() {
    library.addModule(moduleName("A"), "\\data D\n");
    library.addModule(moduleName("B"), "\\import A() \\func f : \\Type0 => A.D\n");
    libraryManager.loadLibrary(library);
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
    libraryManager.unloadLibrary(library);

    library.updateModule(moduleName("A"), "\\data D'", true);
    libraryManager.loadLibrary(library);
    assertThat(errorList, is(not(empty())));
    errorList.clear();
    typechecking.typecheckLibrary(library);
    library.persistUpdateModules(errorReporter);
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
    assertThatErrorsAre(hasErrors(get(aScope, "a")));
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
    library.addModule(moduleName("A"), "\\func a : \\1-Type1 => \\Set0");
    library.addModule(moduleName("B"),
      "\\import A\n" +
      "\\func b : \\Set0 => A.a"); // There is an error here
    libraryManager.loadLibrary(library);
    List<Group> groups = new ArrayList<>(2);
    groups.add(library.getModuleGroup(moduleName("A")));
    groups.add(library.getModuleGroup(moduleName("B")));
    assertTrue(typechecking.typecheckModules(groups));
    assertThat(errorList, hasSize(1));
    errorList.clear();
    library.persistModule(moduleName("B"), IdReferableConverter.INSTANCE, errorReporter);
    assertThat(errorList, is(empty()));

    Source sourceA = library.getBinarySource(moduleName("A"));
    assertThat(sourceA, is(notNullValue()));
    assertFalse(sourceA.isAvailable());
    Source sourceB = library.getBinarySource(moduleName("B"));
    assertThat(sourceB, is(notNullValue()));
    assertTrue(sourceB.isAvailable());
  }
}
