package org.arend.library;

import org.arend.error.ErrorReporter;
import org.arend.module.ModulePath;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.module.scopeprovider.SimpleModuleScopeProvider;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.term.group.ChildGroup;
import org.arend.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Represents a library which cannot be modified after loading.
 */
public abstract class UnmodifiableSourceLibrary extends SourceLibrary {
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

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
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

  @Nonnull
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

  public boolean persistUpdateModules(ErrorReporter errorReporter) {
    boolean ok = true;
    for (ModulePath module : myUpdatedModules) {
      if (!persistModule(module, IdReferableConverter.INSTANCE, errorReporter)) {
        ok = false;
      }
    }
    myUpdatedModules.clear();
    return ok;
  }

  @Nullable
  @Override
  public ChildGroup getModuleGroup(ModulePath modulePath) {
    return myGroups.get(modulePath);
  }
}
