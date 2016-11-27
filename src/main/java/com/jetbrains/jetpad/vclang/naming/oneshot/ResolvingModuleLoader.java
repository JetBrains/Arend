package com.jetbrains.jetpad.vclang.naming.oneshot;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.naming.namespace.*;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;
import java.util.Map;

public class ResolvingModuleLoader<SourceIdT extends SourceId> extends BaseModuleLoader<SourceIdT> {
  private final ResolvingModuleLoadingListener<SourceIdT> myResolvingModuleListener;
  private final Map<ModulePath, Abstract.ClassDefinition> myLoadedModules = new HashMap<>();

  public ResolvingModuleLoader(SourceSupplier<SourceIdT> sourceSupplier, ModuleLoadingListener<SourceIdT> loadingListener, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ResolveListener resolveListener, ErrorReporter errorReporter) {
    this(sourceSupplier, errorReporter, new ResolvingModuleLoadingListener<>(loadingListener, staticNsProvider, dynamicNsProvider, resolveListener, errorReporter));
  }

  private ResolvingModuleLoader(SourceSupplier<SourceIdT> sourceSupplier, ErrorReporter errorReporter, ResolvingModuleLoadingListener<SourceIdT> resolvingModuleListener) {
    super(sourceSupplier, errorReporter, resolvingModuleListener);
    myResolvingModuleListener = resolvingModuleListener;
    myResolvingModuleListener.setModuleLoader(this);
  }

  @Override
  public Abstract.ClassDefinition load(SourceIdT sourceId) {
    if (myLoadedModules.containsKey(sourceId.getModulePath())) {
      throw new IllegalStateException("This path is already loaded");
    }
    Abstract.ClassDefinition result = super.load(sourceId);
    if (result != null) {
      myLoadedModules.put(sourceId.getModulePath(), result);
    }
    return result;
  }

  public void overrideModuleLoader(ModuleLoader moduleLoader) {
    myResolvingModuleListener.setModuleLoader(moduleLoader);
  }

  public void setPreludeNamespace(Namespace namespace) {
    myResolvingModuleListener.setPreludeNamespace(namespace);
  }

  public Abstract.ClassDefinition getLoadedModule(ModulePath modulePath) {
    return myLoadedModules.get(modulePath);
  }


  private static class ResolvingModuleLoadingListener<SourceIdT extends SourceId> extends ModuleLoadingListener<SourceIdT> {
    private final ModuleLoadingListener<SourceIdT> myOriginalLoadingListener;
    private final SimpleModuleNamespaceProvider myModuleNsProvider = new SimpleModuleNamespaceProvider();
    private final OneshotNameResolver myOneshotNameResolver;
    private Namespace myPreludeNamespace = new EmptyNamespace();

    private ResolvingModuleLoadingListener(ModuleLoadingListener<SourceIdT> loadingListener, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ResolveListener resolveListener, ErrorReporter errorReporter) {
      myOriginalLoadingListener = loadingListener;
      myOneshotNameResolver = new OneshotNameResolver(errorReporter, resolveListener, myModuleNsProvider, staticNsProvider, dynamicNsProvider);
    }

    @Override
    public void loadingSucceeded(SourceIdT module, Abstract.ClassDefinition abstractDefinition) {
      myModuleNsProvider.registerModule(module.getModulePath(), abstractDefinition);
      myOneshotNameResolver.visitModule(abstractDefinition, myPreludeNamespace);
      myOriginalLoadingListener.loadingSucceeded(module, abstractDefinition);
    }

    @Override
    public void loadingError(SourceIdT module, ModuleLoadingError loadingError) {
      myOriginalLoadingListener.loadingError(module, loadingError);
    }

    private void setPreludeNamespace(Namespace preludeNamespace) {
      myPreludeNamespace = preludeNamespace;
    }

    private void setModuleLoader(ModuleLoader moduleLoader) {
      myOneshotNameResolver.setModuleLoader(moduleLoader);
    }
  }
}
