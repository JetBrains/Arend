package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

  public void unloadGroup(Group group) {
    unloadDefinition(group.getReferable());
    for (Group subgroup : group.getSubgroups()) {
      unloadGroup(subgroup);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      unloadGroup(subgroup);
    }
  }

  public void unloadDefinition(LocatedReferable referable) {
    if (referable instanceof TCReferable) {
      myTypecheckerState.reset((TCReferable) referable);
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

  public Collection<? extends ModulePath> getUpdatedModules() {
    return Collections.emptyList();
  }

  @Override
  public boolean supportsTypechecking() {
    return true;
  }

  @Override
  public boolean needsTypechecking() {
    return !getUpdatedModules().isEmpty();
  }

  @Override
  public boolean typecheck(Typechecking typechecking) {
    Collection<? extends ModulePath> updatedModules = getUpdatedModules();
    if (updatedModules.isEmpty()) {
      return true;
    }

    List<Group> groups = new ArrayList<>(updatedModules.size());
    for (ModulePath module : updatedModules) {
      Group group = getModuleGroup(module);
      if (group != null) {
        groups.add(group);
      }
    }

    return typechecking.typecheckModules(groups);
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
