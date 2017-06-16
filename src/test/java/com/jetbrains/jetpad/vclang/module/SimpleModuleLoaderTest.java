package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.module.source.SimpleModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.SourceModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.module.ModulePath.moduleName;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SimpleModuleLoaderTest extends VclangTestCase {
  private MemoryStorage storage;
  private Map<MemoryStorage.SourceId, Abstract.ClassDefinition> loadedModules;
  private SourceModuleLoader<MemoryStorage.SourceId> moduleLoader;

  @Before
  public void initialize() {
    storage = new MemoryStorage(null, null);
    loadedModules = new HashMap<>();
    moduleLoader = new SimpleModuleLoader<MemoryStorage.SourceId>(storage, errorReporter) {
      @Override
      public Abstract.ClassDefinition load(MemoryStorage.SourceId sourceId) {
        Abstract.ClassDefinition result = super.load(sourceId);
        if (result != null) {
          Abstract.ClassDefinition old = loadedModules.put(sourceId, result);
          assertThat(old, is(nullValue()));
        }
        return result;
      }
    };
  }

  @Test
  public void loadSimpleModule() {
    storage.add(moduleName("A"), "\\function f => 0");
    Abstract.ClassDefinition result = moduleLoader.load(storage.locateModule(moduleName("A")));
    assertThat(errorList, containsErrors(0));
    assertThat(result, is(notNullValue()));
    assertThat(loadedModules.get(moduleLoader.locateModule(moduleName("A"))), is(equalTo(result)));
  }

  @Test
  public void loadModuleTwice() {
    ModulePath modulePath = moduleName("A");
    storage.add(modulePath, "\\function f => 0");

    MemoryStorage.SourceId source1 = moduleLoader.locateModule(modulePath);
    Abstract.ClassDefinition result1 = moduleLoader.load(source1);

    loadedModules.remove(source1);

    MemoryStorage.SourceId source2 = moduleLoader.locateModule(modulePath);
    assertThat(source2, is(equalTo(source1)));
    Abstract.ClassDefinition result2 = moduleLoader.load(source1);
    // We neither guarantee that result2 == result1 nor the opposite,
    // but loading the same module twice must not be an error.
    assertThat(errorList, containsErrors(0));
    assertThat(result2, is(notNullValue()));
    assertThat(loadedModules.get(source2), is(equalTo(result2)));
  }

  @Test
  public void loadTwoModules() {
    storage.add(moduleName("A"), "\\function f => 0");
    storage.add(moduleName("B"), "\\function g => 0");

    moduleLoader.load(storage.locateModule(moduleName("A")));
    moduleLoader.load(storage.locateModule(moduleName("B")));
    assertThat(errorList, containsErrors(0));

    assertThat(loadedModules.entrySet(), hasSize(2));
  }

  @Test
  public void locateNonExistentModule() {
    MemoryStorage.SourceId source = moduleLoader.locateModule(moduleName("DoesNotExist"));
    assertThat(source, is(nullValue()));
  }

  @Test(expected = IllegalStateException.class)
  public void loadNonExistentSource() {
    storage.add(moduleName("WillBeRemoved"), "");
    MemoryStorage.SourceId source = moduleLoader.locateModule(moduleName("WillBeRemoved"));
    assertThat(source, is(notNullValue()));
    storage.remove(moduleName("WillBeRemoved"));
    assertThat(storage.isAvailable(source), is(false));

    moduleLoader.load(source);
  }

  @Test
  public void moduleWithErrorsError() {
    storage.add(moduleName("A"), "hello world");
    Abstract.ClassDefinition result = moduleLoader.load(storage.locateModule(moduleName("A")));
    assertThat(result, is(nullValue()));
    assertThat(errorList, is(not(empty())));
  }
}
