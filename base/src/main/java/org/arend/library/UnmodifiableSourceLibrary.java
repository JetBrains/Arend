package org.arend.library;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.module.scopeprovider.SimpleModuleScopeProvider;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.term.group.ChildGroup;
import org.arend.typechecking.TypecheckerState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a library which cannot be modified after loading.
 */
public abstract class UnmodifiableSourceLibrary extends SourceLibrary {
  private boolean myExternal = false;
  private final String myName;
  private final SimpleModuleScopeProvider myModuleScopeProvider = new SimpleModuleScopeProvider();
  private final Map<ModulePath, ChildGroup> myGroups = new HashMap<>();
  private final Set<ModulePath> myUpdatedModules = new LinkedHashSet<>();

  /**
   * Creates a new {@code UnmodifiableSourceLibrary}
   *
   * @param name              the name of this library.
   * @param typecheckerState  the underling typechecker state of this library.
   */
  protected UnmodifiableSourceLibrary(String name, TypecheckerState typecheckerState) {
    super(typecheckerState);
    myName = name;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @NotNull
  @Override
  public ModuleScopeProvider getDeclaredModuleScopeProvider() {
    return myModuleScopeProvider;
  }

  @Override
  public void onGroupLoaded(ModulePath modulePath, @Nullable ChildGroup group, boolean isRaw) {
    if (isRaw) {
      if (group == null) {
        myGroups.remove(modulePath);
        myModuleScopeProvider.unregisterModule(modulePath);
        myUpdatedModules.remove(modulePath);
      } else {
        myGroups.put(modulePath, group);
        myModuleScopeProvider.registerModule(modulePath, group);
        myUpdatedModules.add(modulePath);
      }
    }
  }

  @Override
  public void onBinaryLoaded(ModulePath modulePath, boolean isComplete) {
    if (isComplete) {
      myUpdatedModules.remove(modulePath);
    }
  }

  @NotNull
  @Override
  public Collection<? extends ModulePath> getLoadedModules() {
    return myGroups.keySet();
  }

  @Override
  public boolean unload() {
    super.unload();
    myGroups.clear();
    myModuleScopeProvider.clear();
    myUpdatedModules.clear();
    return true;
  }

  @Override
  public void reset() {
    super.reset();
    myUpdatedModules.addAll(getLoadedModules());
  }

  @Override
  public Collection<? extends ModulePath> getUpdatedModules() {
    return myUpdatedModules;
  }

  public void updateModule(ModulePath module) {
    myUpdatedModules.add(module);
  }

  public void updateModules(Collection<? extends ModulePath> modules) {
    myUpdatedModules.addAll(modules);
  }

  public void clearUpdateModules() {
    myUpdatedModules.clear();
  }

  public boolean persistUpdatedModules(ErrorReporter errorReporter) {
    boolean ok = true;
    for (ModulePath module : myUpdatedModules) {
      if (!persistModule(module, IdReferableConverter.INSTANCE, errorReporter)) {
        ok = false;
      }
    }
    myUpdatedModules.clear();
    return ok;
  }

  @Override
  public boolean isExternal() {
    return myExternal;
  }

  public void setExternal(boolean isExternal) {
    myExternal = isExternal;
  }

  @Nullable
  @Override
  public ChildGroup getModuleGroup(ModulePath modulePath) {
    return myGroups.get(modulePath);
  }
}
