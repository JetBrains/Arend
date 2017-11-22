package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Group;
import org.junit.Ignore;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.hasErrors;
import static com.jetbrains.jetpad.vclang.typechecking.Matchers.typeMismatchError;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SimpleSourcelessCachingTest extends SourcelessCachingTestCase {
  @Test
  public void removeSource() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\function a : \\1-Type1 => \\Set0");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "" +
        "\\import A\n" +
        "\\function b : \\1-Type1 => A.a");

    Group bClass = moduleLoader.load(b);
    typecheck(bClass);
    assertThat(errorList, is(empty()));
    persist(b);

    MemoryStorage.Snapshot snapshot = storage.getSnapshot();
    initialize();
    storage.restoreSnapshot(snapshot);

    storage.remove(ModulePath.moduleName("A"));

    bClass = moduleLoader.load(b);
    typecheck(bClass);
    assertThat(errorList, is(empty()));
  }

  @Test
  public void removeSourceWithError() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\function a : \\Set0 => \\Set0");  // There is an error here
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "" +
        "\\import A\n" +
        "\\function b : \\1-Type1 => A.a");

    Group bClass = moduleLoader.load(b);
    typecheck(bClass, 2);
    errorList.clear();
    persist(b);

    MemoryStorage.Snapshot snapshot = storage.getSnapshot();
    initialize();
    storage.restoreSnapshot(snapshot);

    storage.remove(ModulePath.moduleName("A"));
    storage.removeCache(ModulePath.moduleName("B"));

    bClass = moduleLoader.load(b);
    typecheck(bClass, 1);

    Scope aScope = cacheModuleScopeProvider.forModule(ModulePath.moduleName("A"));
    assertThatErrorsAre(hasErrors(get(aScope, "a")));
  }

  @Test
  public void removeSourceErrorInReferrer() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\function a : \\1-Type1 => \\Set0");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "" +
        "\\import A\n" +
        "\\function b : \\Set0 => A.a");  // There is an error here

    Group bClass = moduleLoader.load(b);
    typecheck(bClass, 1);
    errorList.clear();
    persist(a);  // FIXME: B.b has an error in header, so it does not get a reference to A
                 // see: persistDependencyWhenReferrerHasErrorInHeader
    persist(b);

    MemoryStorage.Snapshot snapshot = storage.getSnapshot();
    initialize();
    storage.restoreSnapshot(snapshot);

    storage.remove(ModulePath.moduleName("A"));
    storage.removeCache(ModulePath.moduleName("B"));

    bClass = moduleLoader.load(b);
    typecheck(bClass, 1);

    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  @Ignore
  // This test fails because B.b has an error in its header and, as a result, contains no actual
  // defcalls into A. Therefore, when B is persisted, A is not persisted, although, theoretically,
  // it should be as the cache for B is perfectly valid so is the cache for A. It is not an immediate problem,
  // as the cache of B does not contain any references into A indeed, but it is still an inconsistency.
  public void persistDependencyWhenReferrerHasErrorInHeader() {
    MemoryStorage.SourceId a = storage.add(ModulePath.moduleName("A"), "" +
        "\\function a : \\1-Type1 => \\Set0");
    MemoryStorage.SourceId b = storage.add(ModulePath.moduleName("B"), "" +
        "\\import A\n" +
        "\\function b : \\Set0 => A.a");  // There is an error here

    Group bClass = moduleLoader.load(b);
    typecheck(bClass, 1);
    errorList.clear();
    persist(b);

    MemoryStorage.Snapshot snapshot = storage.getSnapshot();
    assertThat(snapshot.myCaches, hasKey(equalTo(ModulePath.moduleName("B"))));
    assertThat(snapshot.myCaches, hasKey(equalTo(ModulePath.moduleName("A"))));
  }
}
