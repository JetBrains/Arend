package org.arend.library;

import org.arend.ext.ArendExtension;
import org.arend.ext.DefaultArendExtension;
import org.arend.ext.module.ModulePath;
import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.scope.LexicalScope;
import org.arend.term.group.Group;
import org.arend.typechecking.TypecheckerState;
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

  @NotNull
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

  @NotNull
  @Override
  public ArendExtension getArendExtension() {
    return new DefaultArendExtension();
  }

  @NotNull
  @Override
  public ModuleScopeProvider getDeclaredModuleScopeProvider() {
    return module -> {
      Group group = getModuleGroup(module);
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
    return EmptyModuleScopeProvider.INSTANCE;
  }

  public Collection<? extends ModulePath> getUpdatedModules() {
    return Collections.emptyList();
  }

  @Override
  public boolean isExternal() {
    return false;
  }

  private void orderModules(Collection<? extends ModulePath> modules, Ordering ordering) {
    if (modules.isEmpty()) {
      return;
    }

    List<Group> groups = new ArrayList<>(modules.size());
    for (ModulePath module : modules) {
      Group group = getModuleGroup(module);
      if (group != null) {
        groups.add(group);
      }
    }

    ordering.orderModules(groups);
  }

  @Override
  public boolean orderModules(Ordering ordering) {
    orderModules(getUpdatedModules(), ordering);
    return true;
  }

  @Override
  public boolean orderTestModules(Ordering ordering) {
    orderModules(getTestModules(), ordering);
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
