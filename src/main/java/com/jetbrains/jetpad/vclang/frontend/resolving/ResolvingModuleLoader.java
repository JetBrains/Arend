package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.module.DefaultModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashSet;
import java.util.Set;

public class ResolvingModuleLoader<SourceIdT extends SourceId> extends DefaultModuleLoader<SourceIdT> {
  private final ResolvingModuleLoadingListener<SourceIdT> myResolvingModuleListener;
  private final Set<ModulePath> myLoadedModules = new HashSet<>();

  public ResolvingModuleLoader(SourceSupplier<SourceIdT> sourceSupplier, ModuleLoadingListener<SourceIdT> loadingListener, NameResolver nameResolver, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ResolveListener resolveListener, ErrorReporter errorReporter) {
    this(sourceSupplier, errorReporter, new ResolvingModuleLoadingListener<>(nameResolver, staticNsProvider, dynamicNsProvider, loadingListener, resolveListener, errorReporter), nameResolver);
  }

  private ResolvingModuleLoader(SourceSupplier<SourceIdT> sourceSupplier, ErrorReporter errorReporter, ResolvingModuleLoadingListener<SourceIdT> resolvingModuleListener, NameResolver nameResolver) {
    super(sourceSupplier, errorReporter, resolvingModuleListener);
    myResolvingModuleListener = resolvingModuleListener;
    nameResolver.setModuleLoader(this);
  }

  @Override
  public Abstract.ClassDefinition load(SourceIdT sourceId) {
    if (myLoadedModules.contains(sourceId.getModulePath())) {
      throw new IllegalStateException("This path is already loaded");
    }
    Abstract.ClassDefinition result = super.load(sourceId);
    if (result != null) {
      myLoadedModules.add(sourceId.getModulePath());
    }
    return result;
  }

  public void setPreludeNamespace(Namespace namespace) {
    myResolvingModuleListener.setPreludeNamespace(namespace);
  }


  private static class ResolvingModuleLoadingListener<SourceIdT extends SourceId> extends ModuleLoadingListener<SourceIdT> {
    private final ModuleLoadingListener<SourceIdT> myOriginalLoadingListener;
    private final SimpleModuleNamespaceProvider myModuleNsProvider = new SimpleModuleNamespaceProvider();
    private final NameResolver myNameResolver;
    private final NamespaceProviders myNsProviders;
    private final ResolveListener myResolveListener;
    private final ErrorReporter myErrorReporter;
    private Namespace myPreludeNamespace = new EmptyNamespace();

    private ResolvingModuleLoadingListener(NameResolver nameResolver, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ModuleLoadingListener<SourceIdT> loadingListener, ResolveListener resolveListener, ErrorReporter errorReporter) {
      myNameResolver = nameResolver;
      myOriginalLoadingListener = loadingListener;
      myNsProviders = new NamespaceProviders(myModuleNsProvider, staticNsProvider, dynamicNsProvider);
      myResolveListener = resolveListener;
      myErrorReporter = errorReporter;
    }

    @Override
    public void loadingSucceeded(SourceIdT module, Abstract.ClassDefinition abstractDefinition) {
      myModuleNsProvider.registerModule(module.getModulePath(), abstractDefinition);
      OneshotNameResolver.visitModule(abstractDefinition, myPreludeNamespace, myNameResolver, myNsProviders, myResolveListener);
      myOriginalLoadingListener.loadingSucceeded(module, abstractDefinition);
    }

    @Override
    public void loadingError(SourceIdT module, ModuleLoadingError loadingError) {
      myOriginalLoadingListener.loadingError(module, loadingError);
    }

    private void setPreludeNamespace(Namespace preludeNamespace) {
      myPreludeNamespace = preludeNamespace;
    }
  }
}
