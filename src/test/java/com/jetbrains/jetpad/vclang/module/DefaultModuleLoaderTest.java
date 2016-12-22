package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.source.SourceModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.module.ModulePath.moduleName;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class DefaultModuleLoaderTest extends VclangTestCase {
  private MemoryStorage storage;
  private Map<MemoryStorage.SourceId, Abstract.ClassDefinition> loadedModules;
  private SourceModuleLoader<MemoryStorage.SourceId> moduleLoader;
  private List<ModuleLoadingError> loadingErrors;

  @Before
  public void initialize() {
    storage = new MemoryStorage();
    loadedModules = new HashMap<>();
    loadingErrors = new ArrayList<>();
    moduleLoader = new DefaultModuleLoader<>(storage, errorReporter, new DefaultModuleLoader.ModuleLoadingListener<MemoryStorage.SourceId>() {
      @Override
      public void loadingError(MemoryStorage.SourceId module, ModuleLoadingError loadingError) {
        loadingErrors.add(loadingError);
      }
      @Override
      public void loadingSucceeded(MemoryStorage.SourceId module, Abstract.ClassDefinition abstractDefinition) {
        Abstract.ClassDefinition old = loadedModules.put(module, abstractDefinition);
        assertThat(old, is(nullValue()));
      }
    });
  }

  @Test
  public void loadSimpleModule() {
    storage.add(moduleName("A"), "\\function f => f");
    Abstract.ClassDefinition result = moduleLoader.load(moduleName("A"));
    assertThat(errorList, containsErrors(0));
    assertThat(loadingErrors, containsErrors(0));
    assertThat(result, is(notNullValue()));
    assertThat(loadedModules.get(moduleLoader.locateModule(moduleName("A"))), is(equalTo(result)));
  }

  @Test
  public void loadModuleTwice() {
    ModulePath modulePath = moduleName("A");
    storage.add(modulePath, "\\function f => f");

    MemoryStorage.SourceId source1 = moduleLoader.locateModule(modulePath);
    Abstract.ClassDefinition result1 = moduleLoader.load(source1);

    loadedModules.remove(source1);

    MemoryStorage.SourceId source2 = moduleLoader.locateModule(modulePath);
    assertThat(source2, is(equalTo(source1)));
    Abstract.ClassDefinition result2 = moduleLoader.load(source1);
    // We neither guarantee that result2 == result1 nor the opposite,
    // but loading the same module twice must not be an error.
    assertThat(errorList, containsErrors(0));
    assertThat(loadingErrors, containsErrors(0));
    assertThat(result2, is(notNullValue()));
    assertThat(loadedModules.get(source2), is(equalTo(result2)));
  }

  @Test
  public void loadTwoModules() {
    storage.add(moduleName("A"), "\\function f => f");
    storage.add(moduleName("B"), "\\function g => g");

    moduleLoader.load(moduleName("A"));
    moduleLoader.load(moduleName("B"));
    assertThat(errorList, containsErrors(0));
    assertThat(loadingErrors, containsErrors(0));

    assertThat(loadedModules.entrySet(), hasSize(2));
  }

  @Test
  public void locateNonExistentModule() {
    MemoryStorage.SourceId source = moduleLoader.locateModule(moduleName("DoesNotExist"));
    assertThat(source, is(nullValue()));
  }

  @Test
  public void loadNonExistentModule() {
    assertThat(moduleLoader.load(moduleName("DoesNotExist")), is(nullValue()));
    assertThat(errorList, containsErrors(0));
    assertThat(loadingErrors, contains(instanceOf(ModuleNotFoundError.class)));
  }

  @Test
  public void loadNonExistentSource() {
    storage.add(moduleName("WillBeRemoved"), "");
    MemoryStorage.SourceId source = moduleLoader.locateModule(moduleName("WillBeRemoved"));
    assertThat(source, is(notNullValue()));
    storage.remove(moduleName("WillBeRemoved"));
    assertThat(storage.isAvailable(source), is(false));

    assertThat(moduleLoader.load(source), is(nullValue()));
    assertThat(errorList, containsErrors(0));
    assertThat(loadingErrors, contains(instanceOf(ModuleNotFoundError.class)));
  }

  @Test
  public void moduleWithErrorsError() {
    storage.add(moduleName("A"), "hello world");
    Abstract.ClassDefinition result = moduleLoader.load(moduleName("A"));
    assertThat(result, is(nullValue()));
    assertThat(errorList, is(not(empty())));
    assertThat(loadingErrors, contains(instanceOf(ModuleLoadingError.class)));
  }
}
