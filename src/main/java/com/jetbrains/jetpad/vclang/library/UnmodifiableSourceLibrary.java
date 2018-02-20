package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.SimpleGlobalReferable;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.LongName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides an implementation of {@link Library#getModuleScopeProvider} in which the scopes are fixed after loading.
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

  @Nullable
  @Override
  public final Source getRawSource(ModulePath modulePath) {
    return null;
  }

  @Nonnull
  @Override
  protected GlobalReferable generateReferable(ModulePath modulePath, LongName name, Precedence precedence, GlobalReferable typecheckable) {
    return new SimpleGlobalReferable(precedence, name.getLastName(), typecheckable);
  }

  @Nonnull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    return myModuleScopeProvider;
  }

  @Override
  public void onModuleLoaded(ModulePath modulePath, Group group) {
    myGroups.put(modulePath, group);
    myModuleScopeProvider.registerModule(modulePath, group);
  }

  @Nonnull
  @Override
  public Collection<? extends ModulePath> getLoadedModules() {
    return myGroups.keySet();
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
