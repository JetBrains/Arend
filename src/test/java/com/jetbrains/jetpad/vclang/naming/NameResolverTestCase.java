package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.ReferenceConcreteProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.resolving.GroupNameResolver;
import com.jetbrains.jetpad.vclang.naming.resolving.NamespaceProviders;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;

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
  protected final SimpleDynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider(ReferenceConcreteProvider.INSTANCE);
  private   final NamespaceProviders nsProviders = new NamespaceProviders(moduleNsProvider, staticNsProvider, dynamicNsProvider);
  protected final NameResolver nameResolver = new NameResolver(nsProviders);

  @SuppressWarnings("StaticNonFinalField")
  private static Group LOADED_PRELUDE  = null;
  protected Group prelude = null;
  private Scope globalScope = EmptyScope.INSTANCE;

  protected void loadPrelude() {
    if (prelude != null) throw new IllegalStateException();

    if (LOADED_PRELUDE == null) {
      PreludeStorage preludeStorage = new PreludeStorage(nameResolver);

      ListErrorReporter internalErrorReporter = new ListErrorReporter();
      LOADED_PRELUDE = preludeStorage.loadSource(preludeStorage.preludeSourceId, internalErrorReporter).group;
      assertThat("Failed loading Prelude", internalErrorReporter.getErrorList(), containsErrors(0));
    }

    prelude = LOADED_PRELUDE;

    staticNsProvider.collect(prelude, DummyErrorReporter.INSTANCE);
    globalScope = new NamespaceScope(staticNsProvider.forReferable(prelude.getReferable()));
  }


  private Concrete.Expression resolveNamesExpr(Scope parentScope, List<Referable> context, String text, int errors) {
    Concrete.Expression expression = parseExpr(text);
    assertThat(expression, is(notNullValue()));

    expression.accept(new ExpressionResolveNameVisitor(parentScope, context, nameResolver, errorReporter), null);
    assertThat(errorList, containsErrors(errors));
    return expression;
  }

  Concrete.Expression resolveNamesExpr(Scope parentScope, String text, int errors) {
    return resolveNamesExpr(parentScope, new ArrayList<>(), text, errors);
  }

  protected Concrete.Expression resolveNamesExpr(String text, int errors) {
    return resolveNamesExpr(globalScope, new ArrayList<>(), text, errors);
  }

  Concrete.Expression resolveNamesExpr(Scope parentScope, String text) {
    return resolveNamesExpr(parentScope, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(Map<Referable, Binding> context, String text) {
    List<Referable> names = new ArrayList<>(context.keySet());
    return resolveNamesExpr(globalScope, names, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(new HashMap<>(), text);
  }


  GlobalReference resolveNamesDef(String text, int errors) {
    Group group = parseDef(text);
    staticNsProvider.collect(group, errorReporter);
    dynamicNsProvider.collect(group, errorReporter, nameResolver);
    GroupNameResolver groupResolver = new GroupNameResolver(nameResolver, errorReporter, ReferenceConcreteProvider.INSTANCE);
    groupResolver.resolveGroup(group, new MergeScope(new SingletonScope(group.getReferable()), globalScope));
    assertThat(errorList, containsErrors(errors));
    return (GlobalReference) group.getReferable();
  }

  protected GlobalReference resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }


  private void resolveNamesModule(Group group, int errors) {
    GroupNameResolver groupNameResolver = new GroupNameResolver(nameResolver, errorReporter, ReferenceConcreteProvider.INSTANCE);
    groupNameResolver.resolveGroup(group, globalScope);
    assertThat(errorList, containsErrors(errors));
  }

  // FIXME[tests] should be package-private
  protected Group resolveNamesModule(String text, int errors) {
    Group group = parseModule(text);
    staticNsProvider.collect(group, errorReporter);
    dynamicNsProvider.collect(group, errorReporter, nameResolver);
    resolveNamesModule(group, errors);
    return group;
  }

  protected Group resolveNamesModule(String text) {
    return resolveNamesModule(text, 0);
  }


  public GlobalReferable get(GlobalReferable ref, String path) {
    for (String n : path.split("\\.")) {
      GlobalReferable oldRef = ref;

      ref = staticNsProvider.forReferable(oldRef).resolveName(n);
      if (ref != null) continue;

      ref = dynamicNsProvider.forReferable(oldRef).resolveName(n);
      if (ref == null) return null;
    }
    return ref;
  }

}
