package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.caching.CacheLoadingException;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
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
    ChildGroup aClass = moduleLoader.load(a);

    typecheck(aClass, 2);
    errorList.clear();

    Definition.TypeCheckingStatus aStatus = tcState.getTypechecked(get(aClass.getGroupScope(), "a")).status();
    Definition.TypeCheckingStatus b1Status = tcState.getTypechecked(get(aClass.getGroupScope(), "b1")).status();
    Definition.TypeCheckingStatus b2Status = tcState.getTypechecked(get(aClass.getGroupScope(), "b2")).status();

    persist(a);
    tcState.reset();

    load(a);
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "a")).status(), is(equalTo(aStatus)));
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "b1")).status(), is(equalTo(b1Status)));
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "b2")).status(), is(equalTo(b2Status)));
  }

  @Test
  public void circularDependencies() {
    loadPrelude();

    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "\\import B() \\function a (n : Nat) : Nat | zero => zero | suc n => B.b n");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "\\import A() \\function b (n : Nat) : Nat | zero => zero | suc n => A.a n");

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
    ChildGroup aClass = moduleLoader.load(a);

    typecheck(aClass, 1);
    assertThatErrorsAre(goal(0));
    errorList.clear();

    persist(a);

    tcState.reset();
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "b")), is(nullValue()));

    load(a);
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "a")), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "b")), is(notNullValue()));
  }

  @Test
  public void errorInHeader() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\data D\n" +
        "\\function a (d : D) \\with\n" +
        "\\function b : \\Set0 => (\\lam x y => x) \\Prop a");
    ChildGroup aClass = moduleLoader.load(a);

    typecheck(aClass, 2);

    assertThatErrorsAre(typecheckingError(), hasErrors(get(aClass.getGroupScope(), "a")));
    errorList.clear();

    persist(a);

    tcState.reset();
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "D")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "b")), is(nullValue()));

    load(a);
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "D")), is(notNullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "a")), is(nullValue()));
    assertThat(tcState.getTypechecked(get(aClass.getGroupScope(), "b")), is(notNullValue()));
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
      tryLoad(a, false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }

  @Test
  public void dependencySourceChanged() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "\\data D\n");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "\\import A() \\function f : \\Type0 => A.D\n");
    Group bClass = moduleLoader.load(b);
    typecheck(bClass);

    persist(b);
    tcState.reset();

    storage.incVersion(ModulePath.moduleName("A"));
    try {
      tryLoad(b, false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }

  @Test
  public void dependencySourceChangedWithUnload() {
    storage.add(ModulePath.moduleName("A"), "" + "\\function a : \\Set0 => \\Prop");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "\\import A() \\function b : \\Set0 => A.a");

    Group bClass = moduleLoader.load(b);
    typecheck(bClass);
    persist(b);
    tcState.reset();

    moduleLoader.unregisterModule(ModulePath.moduleName("A"));
    moduleLoader.unregisterModule(ModulePath.moduleName("B"));

    storage.incVersion(ModulePath.moduleName("A"));
    moduleLoader.load(b);
    try {
      tryLoad(b, false);
      fail("Exception expected");
    } catch (CacheLoadingException e) {
      assertThat(e.getMessage(), is(equalTo("Source has changed")));
    }
  }
}
