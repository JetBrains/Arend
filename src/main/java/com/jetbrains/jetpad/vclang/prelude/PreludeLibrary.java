package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.LibraryHeader;
import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * A base class for prelude libraries.
 * This class guarantees that prelude is loaded exactly once,
 * so all instances of this class use the same definitions, which are stored in {@link Prelude}.
 */
public abstract class PreludeLibrary extends SourceLibrary {
  private static Group myGroup;
  private static Scope myScope;

  /**
   * Creates a new {@code PreludeLibrary}
   *
   * @param typecheckerState the underling typechecker state of this library.
   */
  protected PreludeLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
  }

  @Override
  public void onModuleLoaded(ModulePath modulePath, @Nullable Group group) {
    if (!modulePath.equals(Prelude.MODULE_PATH)) {
      throw new IllegalStateException();
    }
    myGroup = group;
    myScope = group == null ? null : CachingScope.make(LexicalScope.opened(group));
  }

  @Override
  public boolean load(LibraryManager libraryManager) {
    synchronized (PreludeLibrary.class) {
      if (myScope == null) {
        if (super.load(libraryManager)) {
          Prelude.initialize(myScope, getTypecheckerState());
          return true;
        } else {
          return false;
        }
      }
    }

    Prelude.fillInTypecheckerState(getTypecheckerState());
    setLoaded();
    return true;
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    return new LibraryHeader(Collections.singletonList(Prelude.MODULE_PATH), Collections.emptyList());
  }

  @Nonnull
  @Override
  public String getName() {
    return "prelude";
  }

  @Nonnull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    return module -> module.equals(Prelude.MODULE_PATH) ? myScope : null;
  }

  @Nonnull
  @Override
  public Collection<? extends ModulePath> getLoadedModules() {
    return isLoaded() ? Collections.singletonList(Prelude.MODULE_PATH) : Collections.emptyList();
  }

  @Nullable
  @Override
  public Group getModuleGroup(ModulePath modulePath) {
    return modulePath.equals(Prelude.MODULE_PATH) ? myGroup : null;
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    return modulePath.equals(Prelude.MODULE_PATH);
  }
}
