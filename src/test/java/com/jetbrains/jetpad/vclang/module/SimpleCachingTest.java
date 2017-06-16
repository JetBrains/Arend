package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.term.Abstract;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class SimpleCachingTest extends CachingTestCase {
  @Test
  public void circularDependencies() throws CachePersistenceException {
    loadPrelude();

    storage.add(ModulePath.moduleName("A"), "\\function a (n : Nat) : Nat <= \\elim n | zero => zero | suc n => ::B.b n");
    storage.add(ModulePath.moduleName("B"), "\\function b (n : Nat) : Nat <= \\elim n | zero => zero | suc n => ::A.a n");

    Abstract.ClassDefinition aClass = moduleLoader.load(storage.locateModule(ModulePath.moduleName("A")));
    assertThat(errorList, is(empty()));
    typecheck(aClass);
    assertThat(errorList, is(empty()));
    cacheManager.persistCache(storage.locateModule(ModulePath.moduleName("A")));
    cacheManager.persistCache(storage.locateModule(ModulePath.moduleName("B")));
  }
}
