package org.arend.library;

import org.arend.ext.module.ModulePath;
import org.arend.source.Source;
import org.arend.source.SourceLoader;
import org.arend.term.group.Group;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LibraryLoadingTest extends LibraryTestCase {
  @Test
  public void loadSimpleModule() {
    ModulePath module = new ModulePath("A");
    library.addModule(module, "\\func f => 0");
    assertTrue(libraryManager.loadLibrary(library, null));
    assertThat(library.getModuleGroup(module), is(notNullValue()));
    assertThat(errorList, containsErrors(0));
  }

  @Test
  public void loadModuleTwice() {
    ModulePath modulePath = new ModulePath("A");

    library.addModule(modulePath, "\\func f => 0");
    SourceLoader sourceLoader1 = new SourceLoader(library, libraryManager);
    assertTrue(sourceLoader1.preloadRaw(modulePath, false));
    sourceLoader1.loadRawSources();
    Group result1 = library.getModuleGroup(modulePath);
    assertThat(result1, is(notNullValue()));

    library.addModule(modulePath, "\\func g => 0");
    SourceLoader sourceLoader2 = new SourceLoader(library, libraryManager);
    assertTrue(sourceLoader2.preloadRaw(modulePath, false));
    sourceLoader2.loadRawSources();
    Group result2 = library.getModuleGroup(modulePath);
    assertThat(result2, is(notNullValue()));

    // We neither guarantee that result2 == result1 nor the opposite,
    // but loading the same module twice must not be an error.
    assertThat(errorList, containsErrors(0));
  }

  @Test
  public void loadTwoModules() {
    ModulePath moduleA = new ModulePath("A");
    ModulePath moduleB = new ModulePath("B");
    library.addModule(moduleA, "\\func f => 0");
    library.addModule(moduleB, "\\func g => 0");
    assertTrue(libraryManager.loadLibrary(library, null));
    assertThat(errorList, containsErrors(0));
    assertThat(library.getModuleGroup(moduleA), is(notNullValue()));
    assertThat(library.getModuleGroup(moduleB), is(notNullValue()));
  }

  @Test
  public void locateNonExistentModule() {
    Source source = library.getRawSource(new ModulePath("DoesNotExist"));
    assertThat(source, is(nullValue()));
  }

  @Test
  public void moduleWithErrorsError() {
    ModulePath modulePath = new ModulePath("A");
    library.addModule(modulePath, "hello world");
    assertTrue(libraryManager.loadLibrary(library, null));
    assertThat(library.getModuleGroup(modulePath), is(nullValue()));
    assertThat(errorList, is(not(empty())));
  }
}
