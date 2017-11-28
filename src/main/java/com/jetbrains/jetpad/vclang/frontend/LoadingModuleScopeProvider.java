package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.ModuleRegistry;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LoadingModuleScopeProvider<SourceIdT extends SourceId> extends BaseModuleLoader<SourceIdT> implements ModuleScopeProvider, ModuleRegistry {
  private final SimpleModuleScopeProvider mySourceScopes = new SimpleModuleScopeProvider();

  public LoadingModuleScopeProvider(ErrorReporter errorReporter) {
    super(errorReporter);
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    Scope scope = mySourceScopes.forModule(module);
    if (scope != null) {
      return scope;
    } else {
      ChildGroup res = load(module);
      if (res != null) {
        return mySourceScopes.forModule(module);
      } else {
        return null;
      }
    }
  }

  @Override
  protected void loadingSucceeded(SourceIdT module, SourceSupplier.LoadResult result) {
    mySourceScopes.registerModule(module.getModulePath(), result.group);
  }

  @Override
  public void registerModule(ModulePath modulePath, Group group) {
    mySourceScopes.registerModule(modulePath, group);
  }

  @Override
  public void unregisterModule(ModulePath path) {
    mySourceScopes.unregisterModule(path);
  }

  @Override
  public boolean isRegistered(ModulePath modulePath) {
    return mySourceScopes.isRegistered(modulePath);
  }
}
