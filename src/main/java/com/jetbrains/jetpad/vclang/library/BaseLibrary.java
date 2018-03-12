package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;

/**
 * Provides a basic implementation of some of the methods of {@link Library}.
 */
public abstract class BaseLibrary implements Library {
  private final TypecheckerState myTypecheckerState;
  private boolean myLoaded = false;

  /**
   * Creates a new {@code BaseLibrary}
   *
   * @param typecheckerState  the underling typechecker state of this library.
   */
  protected BaseLibrary(TypecheckerState typecheckerState) {
    myTypecheckerState = typecheckerState;
  }

  @Nonnull
  @Override
  public TypecheckerState getTypecheckerState() {
    return myTypecheckerState;
  }

  @Override
  public boolean load(LibraryManager libraryManager) {
    myLoaded = true;
    return true;
  }

  protected void setLoaded() {
    myLoaded = true;
  }

  @Override
  public void unload() {
    for (ModulePath modulePath : getLoadedModules()) {
      Group group = getModuleGroup(modulePath);
      if (group != null) {
        unloadGroup(group);
      }
    }
    myLoaded = false;
  }

  private void unloadGroup(Group group) {
    myTypecheckerState.reset(group.getReferable());
    for (Group subgroup : group.getSubgroups()) {
      unloadGroup(subgroup);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      unloadGroup(subgroup);
    }
  }

  @Override
  public boolean isLoaded() {
    return myLoaded;
  }

  @Nonnull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    return module -> {
      Group group = getModuleGroup(module);
      return group == null ? null : LexicalScope.opened(group);
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SourceLibrary that = (SourceLibrary) o;

    return getName().equals(that.getName());
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @Override
  public String toString() {
    return getName();
  }
}
