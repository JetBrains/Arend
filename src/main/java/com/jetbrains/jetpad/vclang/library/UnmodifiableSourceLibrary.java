package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a library which cannot be modified after loading.
 */
public abstract class UnmodifiableSourceLibrary extends SourceLibrary {
  private final String myName;
  private final SimpleModuleScopeProvider myModuleScopeProvider = new SimpleModuleScopeProvider();
  private final Map<ModulePath, Group> myGroups = new HashMap<>();

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
  public void onModuleLoaded(ModulePath modulePath, @Nullable Group group) {
    myGroups.put(modulePath, group);
    if (group == null) {
      myModuleScopeProvider.unregisterModule(modulePath);
    } else {
      myModuleScopeProvider.registerModule(modulePath, group);
    }
  }

  @Nonnull
  @Override
  public Collection<? extends ModulePath> getLoadedModules() {
    return myGroups.keySet();
  }

  @Override
  public void unload() {
    super.unload();
    myGroups.clear();
    myModuleScopeProvider.clear();
  }

  @Nullable
  @Override
  public Group getModuleGroup(ModulePath modulePath) {
    return myGroups.get(modulePath);
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    return myGroups.containsKey(modulePath);
  }
}
