package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.source.SourceLoader;
import com.jetbrains.jetpad.vclang.term.group.Group;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.module.ModulePath.moduleName;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class LibraryLoadingTest extends LibraryTestCase {
  @Test
  public void loadSimpleModule() {
    ModulePath module = moduleName("A");
    library.addModule(module, "\\func f => 0");
    assertTrue(libraryManager.loadLibrary(library));
    assertThat(library.getModuleGroup(module), is(notNullValue()));
    assertThat(errorList, containsErrors(0));
  }

  @Test
  public void loadModuleTwice() {
    ModulePath modulePath = moduleName("A");

    library.addModule(modulePath, "\\func f => 0");
    Source source1 = library.getRawSource(modulePath);
    assertThat(source1, is(notNullValue()));
    assertTrue(source1.load(new SourceLoader(library, libraryManager, false)));
    Group result1 = library.getModuleGroup(modulePath);
    assertThat(result1, is(notNullValue()));

    library.addModule(modulePath, "\\func g => 0");
    Source source2 = library.getRawSource(modulePath);
    assertThat(source2, is(notNullValue()));
    assertTrue(source2.load(new SourceLoader(library, libraryManager, false)));
    Group result2 = library.getModuleGroup(modulePath);
    assertThat(result2, is(notNullValue()));

    // We neither guarantee that result2 == result1 nor the opposite,
    // but loading the same module twice must not be an error.
    assertThat(errorList, containsErrors(0));
  }

  @Test
  public void loadTwoModules() {
    ModulePath moduleA = moduleName("A");
    ModulePath moduleB = moduleName("B");
    library.addModule(moduleA, "\\func f => 0");
    library.addModule(moduleB, "\\func g => 0");
    assertTrue(libraryManager.loadLibrary(library));
    assertThat(errorList, containsErrors(0));
    assertThat(library.getModuleGroup(moduleA), is(notNullValue()));
    assertThat(library.getModuleGroup(moduleB), is(notNullValue()));
  }

  @Test
  public void locateNonExistentModule() {
    Source source = library.getRawSource(moduleName("DoesNotExist"));
    assertThat(source, is(nullValue()));
  }

  @Test
  public void moduleWithErrorsError() {
    ModulePath modulePath = moduleName("A");
    library.addModule(modulePath, "hello world");
    assertFalse(libraryManager.loadLibrary(library));
    assertThat(library.getModuleGroup(modulePath), is(nullValue()));
    assertThat(errorList, is(not(empty())));
  }
}
