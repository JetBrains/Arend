package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class OneshotNameResolver {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final ResolveListener myResolveListener;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;

  public OneshotNameResolver(ErrorReporter errorReporter, ResolveListener listener, ModuleNamespaceProvider moduleNsProvider, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider) {
    myErrorReporter = errorReporter;
    myNameResolver = new NameResolver(moduleNsProvider, staticNsProvider, dynamicNsProvider);
    myResolveListener = listener;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
  }

  public void visitModule(Abstract.ClassDefinition module, Scope globalScope) {
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, globalScope, myNameResolver, myErrorReporter, myResolveListener);
    visitor.visitClass(module, null);
  }

  public void setModuleLoader(ModuleLoader moduleLoader) {
    myNameResolver.setModuleLoader(moduleLoader);
  }
}
