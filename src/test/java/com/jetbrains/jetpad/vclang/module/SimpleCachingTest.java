package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class SimpleCachingTest extends CachingTestCase {
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
}
