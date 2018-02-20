package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.frontend.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.module.scopeprovider.EmptyModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.group.Group;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.module.ModulePath.moduleName;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BaseModuleLoaderTest extends VclangTestCase {
  private MemoryStorage storage;
  private BaseModuleLoader<MemoryStorage.SourceId> moduleLoader;
  private final Map<MemoryStorage.SourceId, Group> loadedModules = new HashMap<>();
  private final Set<MemoryStorage.SourceId> failedModules = new HashSet<>();

  @Before
  public void initialize() {
    moduleLoader = new BaseModuleLoader<MemoryStorage.SourceId>(errorReporter) {
      @Override
      protected void loadingSucceeded(MemoryStorage.SourceId module, SourceSupplier.LoadResult result) {
        Group old = loadedModules.put(module, result.group);
        assertThat(old, is(nullValue()));
      }

      @Override
      protected void loadingFailed(MemoryStorage.SourceId module) {
        failedModules.add(module);
      }
    };
    storage = new MemoryStorage(new SimpleModuleScopeProvider(), EmptyModuleScopeProvider.INSTANCE);
    moduleLoader.setSourceSupplier(storage);
  }

  protected Group load(MemoryStorage.SourceId sourceId) {
    Group result = moduleLoader.load(sourceId);
    if (result != null) {
      assertThat(loadedModules, hasKey(equalTo(sourceId)));
    } else {
      assertThat(failedModules, contains(sourceId));
    }
    return result;
  }

  @Test
  public void loadSimpleModule() {
    storage.add(moduleName("A"), "\\func f => 0");
    Group result = load(storage.locateModule(moduleName("A")));
    assertThat(errorList, containsErrors(0));
    assertThat(result, is(notNullValue()));
    assertThat(loadedModules.get(storage.locateModule(moduleName("A"))).getReferable(), is(equalTo(result.getReferable())));
  }

  @Test
  public void loadModuleTwice() {
    ModulePath modulePath = moduleName("A");
    storage.add(modulePath, "\\func f => 0");

    MemoryStorage.SourceId source1 = storage.locateModule(modulePath);
    Group result1 = load(source1);

    loadedModules.remove(source1);

    MemoryStorage.SourceId source2 = storage.locateModule(modulePath);
    assertThat(source2, is(equalTo(source1)));
    Group result2 = load(source1);
    // We neither guarantee that result2 == result1 nor the opposite,
    // but loading the same module twice must not be an error.
    assertThat(errorList, containsErrors(0));
    assertThat(result2, is(notNullValue()));
    assertThat(loadedModules.get(source2).getReferable(), is(equalTo(result2.getReferable())));
  }

  @Test
  public void loadTwoModules() {
    storage.add(moduleName("A"), "\\func f => 0");
    storage.add(moduleName("B"), "\\func g => 0");

    load(storage.locateModule(moduleName("A")));
    load(storage.locateModule(moduleName("B")));
    assertThat(errorList, containsErrors(0));

    assertThat(loadedModules.entrySet(), hasSize(2));
  }

  @Test
  public void locateNonExistentModule() {
    MemoryStorage.SourceId source = storage.locateModule(moduleName("DoesNotExist"));
    assertThat(source, is(nullValue()));
  }

  @Test
  public void loadNonExistentSource() {
    storage.add(moduleName("WillBeRemoved"), "");
    MemoryStorage.SourceId source = storage.locateModule(moduleName("WillBeRemoved"));
    assertThat(source, is(notNullValue()));
    storage.remove(moduleName("WillBeRemoved"));
    assertThat(storage.isAvailable(source), is(false));

    Group result = load(source);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void moduleWithErrorsError() {
    storage.add(moduleName("A"), "hello world");
    Group result = load(storage.locateModule(moduleName("A")));
    assertThat(result, is(nullValue()));
    assertThat(errorList, is(not(empty())));
  }
}
