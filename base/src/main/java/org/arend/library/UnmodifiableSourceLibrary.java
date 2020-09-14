package org.arend.library;

import org.arend.ext.module.ModulePath;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.module.scopeprovider.SimpleModuleScopeProvider;
import org.arend.term.group.ChildGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class UnmodifiableSourceLibrary extends SourceLibrary {
  private final String myName;
  private final SimpleModuleScopeProvider myModuleScopeProvider = new SimpleModuleScopeProvider();
  private final Map<ModulePath, ChildGroup> myGroups = new HashMap<>();
  private final Map<ModulePath, ChildGroup> myTestGroups = new HashMap<>();

  protected UnmodifiableSourceLibrary(String name) {
    myName = name;
  }

  @Override
  public @NotNull String getName() {
    return myName;
  }

  public String getFullName() {
    return myName;
  }

  @NotNull
  @Override
  public ModuleScopeProvider getDeclaredModuleScopeProvider() {
    return myModuleScopeProvider;
  }

  @Override
  public void groupLoaded(ModulePath modulePath, @Nullable ChildGroup group, boolean isRaw, boolean inTests) {
    if (isRaw) {
      Map<ModulePath, ChildGroup> groups = inTests ? myTestGroups : myGroups;
      if (group == null) {
        groups.remove(modulePath);
        myModuleScopeProvider.unregisterModule(modulePath);
      } else {
        groups.put(modulePath, group);
        myModuleScopeProvider.registerModule(modulePath, group);
      }
    }
  }

  @Override
  public @NotNull Collection<? extends ModulePath> getLoadedModules() {
    return myGroups.keySet();
  }

  @Override
  public boolean unload() {
    super.unload();
    myGroups.clear();
    myModuleScopeProvider.clear();
    return true;
  }

  @Override
  public @Nullable ChildGroup getModuleGroup(ModulePath modulePath, boolean inTests) {
    return (inTests ? myTestGroups : myGroups).get(modulePath);
  }
}
