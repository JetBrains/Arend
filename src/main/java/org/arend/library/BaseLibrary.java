package org.arend.library;

import org.arend.ext.ArendExtension;
import org.arend.ext.module.ModulePath;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.scope.LexicalScope;
import org.arend.term.group.Group;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.order.Ordering;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  public boolean load(LibraryManager libraryManager, TypecheckingOrderingListener typechecking) {
    myLoaded = true;
    return true;
  }

  protected void setLoaded() {
    myLoaded = true;
  }

  @Override
  public boolean unload() {
    reset();
    myLoaded = false;
    return true;
  }

  @Override
  public void reset() {
    for (ModulePath modulePath : getLoadedModules()) {
      Group group = getModuleGroup(modulePath);
      if (group != null) {
        resetGroup(group);
      }
    }
  }

  public void resetGroup(Group group) {
    resetDefinition(group.getReferable());
    for (Group subgroup : group.getSubgroups()) {
      resetGroup(subgroup);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      resetGroup(subgroup);
    }
  }

  public void resetDefinition(LocatedReferable referable) {
    if (referable instanceof TCReferable) {
      myTypecheckerState.reset((TCReferable) referable);
    }
  }

  @Override
  public boolean isLoaded() {
    return myLoaded;
  }

  @Nullable
  @Override
  public ArendExtension getArendExtension() {
    return null;
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
  public boolean isExternal() {
    return false;
  }

  @Override
  public boolean orderModules(Ordering ordering) {
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

    ordering.orderModules(groups);
    return true;
  }

  @Override
  public String toString() {
    return getName();
  }
}
