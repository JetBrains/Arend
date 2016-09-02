package com.jetbrains.jetpad.vclang.naming.oneshot;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;

public class OneshotNameResolver {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final ResolveListener myResolveListener;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;

  public OneshotNameResolver(ErrorReporter errorReporter, NameResolver nameResolver, ResolveListener listener, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider) {
    myErrorReporter = errorReporter;
    myNameResolver = nameResolver;
    myResolveListener = listener;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
  }

  public void visitModule(Abstract.ClassDefinition module) {
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, Prelude.PRELUDE, myNameResolver, myErrorReporter, myResolveListener);
    visitor.visitClass(module, null);
  }
}
