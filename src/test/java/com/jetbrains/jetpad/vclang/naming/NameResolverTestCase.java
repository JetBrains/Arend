package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.resolving.NamespaceProviders;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.TestLocalErrorReporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class NameResolverTestCase extends ParserTestCase {
  protected final SimpleModuleNamespaceProvider moduleNsProvider  = new SimpleModuleNamespaceProvider();
  protected final SimpleStaticNamespaceProvider staticNsProvider  = new SimpleStaticNamespaceProvider();
  private   final NamespaceProviders nsProviders = new NamespaceProviders(moduleNsProvider, staticNsProvider, null);
  protected final NameResolver nameResolver = new NameResolver(nsProviders);
  protected final SimpleModuleScopeProvider moduleScopeProvider = new SimpleModuleScopeProvider();

  @SuppressWarnings("StaticNonFinalField")
  private static Group LOADED_PRELUDE  = null;
  protected Group prelude = null;

  protected void loadPrelude() {
    if (prelude != null) throw new IllegalStateException();

    if (LOADED_PRELUDE == null) {
      PreludeStorage preludeStorage = new PreludeStorage(moduleScopeProvider);

      ListErrorReporter internalErrorReporter = new ListErrorReporter();
      LOADED_PRELUDE = preludeStorage.loadSource(preludeStorage.preludeSourceId, internalErrorReporter).group;
      assertThat("Failed loading Prelude", internalErrorReporter.getErrorList(), containsErrors(0));
    }

    prelude = LOADED_PRELUDE;

    staticNsProvider.collect(prelude, DummyErrorReporter.INSTANCE);
  }


  private Concrete.Expression resolveNamesExpr(Scope parentScope, List<Referable> context, String text, int errors) {
    Concrete.Expression expression = parseExpr(text);
    assertThat(expression, is(notNullValue()));

    expression.accept(new ExpressionResolveNameVisitor(parentScope, context, new TestLocalErrorReporter(errorReporter)), null);
    assertThat(errorList, containsErrors(errors));
    return expression;
  }

  Concrete.Expression resolveNamesExpr(Scope parentScope, String text, int errors) {
    return resolveNamesExpr(parentScope, new ArrayList<>(), text, errors);
  }

  protected Concrete.Expression resolveNamesExpr(String text, int errors) {
    return resolveNamesExpr(new CachingScope(ScopeFactory.forGroup(null, moduleScopeProvider)), new ArrayList<>(), text, errors);
  }

  Concrete.Expression resolveNamesExpr(Scope parentScope, String text) {
    return resolveNamesExpr(parentScope, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(Map<Referable, Binding> context, String text) {
    List<Referable> names = new ArrayList<>(context.keySet());
    return resolveNamesExpr(new CachingScope(ScopeFactory.forGroup(null, moduleScopeProvider)), names, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(new HashMap<>(), text);
  }


  ConcreteGlobalReferable resolveNamesDef(String text, int errors) {
    ChildGroup group = parseDef(text);
    staticNsProvider.collect(group, errorReporter);
    new DefinitionResolveNameVisitor(errorReporter).resolveGroup(group, new CachingScope(ScopeFactory.forGroup(group, moduleScopeProvider)), ConcreteReferableProvider.INSTANCE);
    assertThat(errorList, containsErrors(errors));
    return (ConcreteGlobalReferable) group.getReferable();
  }

  protected ConcreteGlobalReferable resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }


  private void resolveNamesModule(ChildGroup group, int errors) {
    new DefinitionResolveNameVisitor(errorReporter).resolveGroup(group, new CachingScope(ScopeFactory.forGroup(group, moduleScopeProvider)), ConcreteReferableProvider.INSTANCE);
    assertThat(errorList, containsErrors(errors));
  }

  // FIXME[tests] should be package-private
  protected Group resolveNamesModule(String text, int errors) {
    ChildGroup group = parseModule(text);
    staticNsProvider.collect(group, errorReporter);
    resolveNamesModule(group, errors);
    return group;
  }

  protected Group resolveNamesModule(String text) {
    return resolveNamesModule(text, 0);
  }


  public GlobalReferable get(GlobalReferable ref, String path) {
    for (String n : path.split("\\.")) {
      ref = staticNsProvider.forReferable(ref).resolveName(n);
      if (ref == null) return null;
    }
    return ref;
  }

}
