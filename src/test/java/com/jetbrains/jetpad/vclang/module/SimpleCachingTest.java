package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.caching.CacheLoadingException;
import com.jetbrains.jetpad.vclang.term.Abstract;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SimpleCachingTest extends CachingTestCase {
  @Test
  public void statusSerialization() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\function a : \\Set0 => \\Prop\n" +
        "\\function b1 : \\Set0 => \\Set0\n" +
        "\\function b2 : \\Set0 => b1");
    Abstract.ClassDefinition aClass = moduleLoader.load(a);

    typecheck(aClass, 2);
    errorList.clear();

    Definition.TypeCheckingStatus aStatus = tcState.getTypechecked(get(aClass, "a")).status();
    Definition.TypeCheckingStatus b1Status = tcState.getTypechecked(get(aClass, "b1")).status();
    Definition.TypeCheckingStatus b2Status = tcState.getTypechecked(get(aClass, "b2")).status();

    persist(a);
    tcState.reset();

    load(a, aClass);
    assertThat(tcState.getTypechecked(get(aClass, "a")).status(), is(equalTo(aStatus)));
    assertThat(tcState.getTypechecked(get(aClass, "b1")).status(), is(equalTo(b1Status)));
    assertThat(tcState.getTypechecked(get(aClass, "b2")).status(), is(equalTo(b2Status)));
  }

  @Test
  public void circularDependencies() {
    loadPrelude();

    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "\\function a (n : Nat) : Nat <= \\elim n | zero => zero | suc n => ::B.b n");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "\\function b (n : Nat) : Nat <= \\elim n | zero => zero | suc n => ::A.a n");

    Abstract.ClassDefinition aClass = moduleLoader.load(a);
    typecheck(aClass);
    assertThat(errorList, is(empty()));
    persist(a);
    persist(b);
  }

  @Test
  public void errorInBody() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\function a : \\Set0 => b\n" +
        "\\function b : \\Set0 => {?}");
    Abstract.ClassDefinition aClass = moduleLoader.load(a);

    typecheck(aClass, 1);
    assertThatErrorsAre(goal(0));
    errorList.clear();

    persist(a);

    tcState.reset();
    assertThat(tcState.getTypechecked(aClass), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "b")), is(nullValue()));

    load(a, aClass);
    assertThat(tcState.getTypechecked(aClass), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "a")), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "b")), is(notNullValue()));
  }

  @Test
  public void errorInHeader() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\data D\n" +
        "\\function a (d : D) <= \\elim d\n" +
        "\\function b : \\Set0 => (\\lam x y => x) \\Prop a");
    Abstract.ClassDefinition aClass = moduleLoader.load(a);

    typecheck(aClass, 2);

    assertThatErrorsAre(typecheckingError(), hasErrors(get(aClass, "a")));
    errorList.clear();

    persist(a);

    tcState.reset();
    assertThat(tcState.getTypechecked(aClass), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "D")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "b")), is(nullValue()));

    load(a, aClass);
    assertThat(tcState.getTypechecked(aClass), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "D")), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass, "b")), is(notNullValue()));
  }

  @Test
  public void sourceChanged() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "\\data D\n");
    Abstract.ClassDefinition aClass = moduleLoader.load(a);
    typecheck(aClass);

    persist(a);
    tcState.reset();

    storage.incVersion(ModulePath.moduleName("A"));
    try {
      tryLoad(a, aClass, false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }

  @Test
  public void dependencySourceChanged() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "\\data D\n");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "\\function f : \\Type0 => ::A.D\n");
    Abstract.ClassDefinition bClass = moduleLoader.load(b);
    typecheck(bClass);

    persist(b);
    tcState.reset();

    storage.incVersion(ModulePath.moduleName("A"));
    try {
      tryLoad(b, bClass, false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }

  @Test
  public void dependencySourceChangedWithUnload() {
    storage.add(ModulePath.moduleName("A"), "" + "\\function a : \\Set0 => \\Prop");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "" + "\\function b : \\Set0 => ::A.a");

    Abstract.ClassDefinition bClass = moduleLoader.load(b);
    typecheck(bClass);
    persist(b);
    tcState.reset();

    moduleNsProvider.unregisterModule(ModulePath.moduleName("A"));
    moduleNsProvider.unregisterModule(ModulePath.moduleName("B"));

    storage.incVersion(ModulePath.moduleName("A"));
    bClass = moduleLoader.load(b);
    try {
      tryLoad(b, bClass, false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }
}
