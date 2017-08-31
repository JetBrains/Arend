package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.caching.CacheLoadingException;
import com.jetbrains.jetpad.vclang.term.Group;
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
    Group aClass = moduleLoader.load(a);

    typecheck(aClass, 2);
    errorList.clear();

    Definition.TypeCheckingStatus aStatus = tcState.getTypechecked(get(aClass.getReferable(), "a")).status();
    Definition.TypeCheckingStatus b1Status = tcState.getTypechecked(get(aClass.getReferable(), "b1")).status();
    Definition.TypeCheckingStatus b2Status = tcState.getTypechecked(get(aClass.getReferable(), "b2")).status();

    persist(a);
    tcState.reset();

    load(a, aClass.getReferable());
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "a")).status(), is(equalTo(aStatus)));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "b1")).status(), is(equalTo(b1Status)));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "b2")).status(), is(equalTo(b2Status)));
  }

  @Test
  public void circularDependencies() {
    loadPrelude();

    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "\\function a (n : Nat) : Nat | zero => zero | suc n => ::B.b n");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "\\function b (n : Nat) : Nat | zero => zero | suc n => ::A.a n");

    Group aClass = moduleLoader.load(a);
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
    Group aClass = moduleLoader.load(a);

    typecheck(aClass, 1);
    assertThatErrorsAre(goal(0));
    errorList.clear();

    persist(a);

    tcState.reset();
    assertThat(tcState.getTypechecked(aClass.getReferable()), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "b")), is(nullValue()));

    load(a, aClass.getReferable());
    assertThat(tcState.getTypechecked(aClass.getReferable()), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "a")), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "b")), is(notNullValue()));
  }

  @Test
  public void errorInHeader() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\data D\n" +
        "\\function a (d : D) \\with\n" +
        "\\function b : \\Set0 => (\\lam x y => x) \\Prop a");
    Group aClass = moduleLoader.load(a);

    typecheck(aClass, 2);

    assertThatErrorsAre(typecheckingError(), hasErrors(get(aClass.getReferable(), "a")));
    errorList.clear();

    persist(a);

    tcState.reset();
    assertThat(tcState.getTypechecked(aClass.getReferable()), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "D")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "b")), is(nullValue()));

    load(a, aClass.getReferable());
    assertThat(tcState.getTypechecked(aClass.getReferable()), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "D")), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getReferable(), "b")), is(notNullValue()));
  }

  @Test
  public void sourceChanged() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "\\data D\n");
    Group aClass = moduleLoader.load(a);
    typecheck(aClass);

    persist(a);
    tcState.reset();

    storage.incVersion(ModulePath.moduleName("A"));
    try {
      tryLoad(a, aClass.getReferable(), false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }

  @Test
  public void dependencySourceChanged() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "\\data D\n");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "\\function f : \\Type0 => ::A.D\n");
    Group bClass = moduleLoader.load(b);
    typecheck(bClass);

    persist(b);
    tcState.reset();

    storage.incVersion(ModulePath.moduleName("A"));
    try {
      tryLoad(b, bClass.getReferable(), false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }

  @Test
  public void dependencySourceChangedWithUnload() {
    storage.add(ModulePath.moduleName("A"), "" + "\\function a : \\Set0 => \\Prop");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "" + "\\function b : \\Set0 => ::A.a");

    Group bClass = moduleLoader.load(b);
    typecheck(bClass);
    persist(b);
    tcState.reset();

    moduleNsProvider.unregisterModule(ModulePath.moduleName("A"));
    moduleNsProvider.unregisterModule(ModulePath.moduleName("B"));

    storage.incVersion(ModulePath.moduleName("A"));
    bClass = moduleLoader.load(b);
    try {
      tryLoad(b, bClass.getReferable(), false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }
}
