package org.arend.library;

import org.arend.ext.ArendExtension;
import org.arend.ext.DefaultArendExtension;
import org.arend.ext.module.ModulePath;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.scope.LexicalScope;
import org.arend.term.group.Group;
import org.arend.typechecking.order.Ordering;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Provides a basic implementation of some of the methods of {@link Library}.
 */
public abstract class BaseLibrary implements Library {
  private boolean myLoaded = false;

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
      Group group = getModuleGroup(modulePath, false);
      if (group != null) {
        resetGroup(group);
      }
    }
    for (ModulePath modulePath : getTestModules()) {
      Group group = getModuleGroup(modulePath, true);
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
    if (referable instanceof TCDefReferable) {
      ((TCDefReferable) referable).setTypechecked(null);
    }
  }

  @Override
  public boolean isLoaded() {
    return myLoaded;
  }

  @NotNull
  @Override
  public ArendExtension getArendExtension() {
    return new DefaultArendExtension();
  }

  @NotNull
  @Override
  public ModuleScopeProvider getDeclaredModuleScopeProvider() {
    return module -> {
      Group group = getModuleGroup(module, false);
      return group == null ? null : LexicalScope.opened(group);
    };
  }

  @NotNull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    return getDeclaredModuleScopeProvider();
  }

  @Override
  public @NotNull ModuleScopeProvider getTestsModuleScopeProvider() {
    return module -> {
      Group group = getModuleGroup(module, true);
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

  private void orderModules(Collection<? extends ModulePath> modules, Ordering ordering, boolean inTests) {
    if (modules.isEmpty()) {
      return;
    }

    List<Group> groups = new ArrayList<>(modules.size());
    for (ModulePath module : modules) {
      Group group = getModuleGroup(module, inTests);
      if (group != null) {
        groups.add(group);
      }
    }

    ordering.orderModules(groups);
  }

  @Override
  public boolean orderModules(Ordering ordering) {
    orderModules(getUpdatedModules(), ordering, false);
    return true;
  }

  @Override
  public boolean orderTestModules(Ordering ordering) {
    orderModules(getTestModules(), ordering, true);
    return true;
  }

  @Override
  public @NotNull Collection<? extends ModulePath> getTestModules() {
    return Collections.emptyList();
  }

  @Override
  public boolean loadTests(LibraryManager libraryManager) {
    return false;
  }

  @Override
  public String toString() {
    return getName();
  }
}
