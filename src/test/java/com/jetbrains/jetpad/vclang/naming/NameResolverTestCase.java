package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.frontend.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.resolving.NamespaceProviders;
import com.jetbrains.jetpad.vclang.frontend.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.frontend.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

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
  protected final SimpleDynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();
  private   final NamespaceProviders nsProviders = new NamespaceProviders(moduleNsProvider, staticNsProvider, dynamicNsProvider);
  protected final NameResolver nameResolver = new NameResolver(nsProviders);

  @SuppressWarnings("StaticNonFinalField")
  private static Abstract.ClassDefinition LOADED_PRELUDE  = null;
  protected Abstract.ClassDefinition prelude = null;
  private Scope globalScope = new EmptyScope();

  protected void loadPrelude() {
    if (prelude != null) throw new IllegalStateException();

    if (LOADED_PRELUDE == null) {
      PreludeStorage preludeStorage = new PreludeStorage(nameResolver);

      ListErrorReporter internalErrorReporter = new ListErrorReporter();
      LOADED_PRELUDE = preludeStorage.loadSource(preludeStorage.preludeSourceId, internalErrorReporter).definition;
      assertThat("Failed loading Prelude", internalErrorReporter.getErrorList(), containsErrors(0));
    }

    prelude = LOADED_PRELUDE;

    globalScope = new NamespaceScope(staticNsProvider.forDefinition(prelude));
  }


  private Concrete.Expression resolveNamesExpr(Scope parentScope, List<Abstract.ReferableSourceNode> context, String text, int errors) {
    Concrete.Expression expression = parseExpr(text);
    assertThat(expression, is(notNullValue()));

    expression.accept(new ExpressionResolveNameVisitor(parentScope, context, nameResolver, new ConcreteResolveListener(), errorReporter), null);
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

  protected Concrete.Expression resolveNamesExpr(Map<Abstract.ReferableSourceNode, Binding> context, String text) {
    List<Abstract.ReferableSourceNode> names = new ArrayList<>(context.keySet());
    return resolveNamesExpr(globalScope, names, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(new HashMap<>(), text);
  }


  private void resolveNamesDef(Concrete.Definition definition, int errors) {
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(nameResolver, Concrete.NamespaceCommandStatement.GET, new ConcreteResolveListener(), errorReporter);
    definition.accept(visitor, new OverridingScope(globalScope, new NamespaceScope(new SimpleNamespace(definition))));
    assertThat(errorList, containsErrors(errors));
  }

  Concrete.Definition resolveNamesDef(String text, int errors) {
    Concrete.Definition result = parseDef(text);
    resolveNamesDef(result, errors);
    return result;
  }

  protected Concrete.Definition resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }


  private void resolveNamesClass(Concrete.ClassDefinition classDefinition, int errors) {
    resolveNamesDef(classDefinition, errors);
  }

  // FIXME[tests] should be package-private
  protected Concrete.ClassDefinition resolveNamesClass(String text, int errors) {
    Concrete.ClassDefinition classDefinition = parseClass("$testClass$", text);
    resolveNamesClass(classDefinition, errors);
    return classDefinition;
  }

  protected Concrete.ClassDefinition resolveNamesClass(String text) {
    return resolveNamesClass(text, 0);
  }


  public Abstract.Definition get(Abstract.Definition ref, String path) {
    for (String n : path.split("\\.")) {
      Abstract.Definition oldref = ref;

      ref = staticNsProvider.forDefinition(oldref).resolveName(n);
      if (ref != null) continue;

      if (oldref instanceof Abstract.ClassDefinition) {
        ref = dynamicNsProvider.forClass((Abstract.ClassDefinition) oldref).resolveName(n);
      } else if (oldref instanceof ClassDefinition) {
        ref = dynamicNsProvider.forClass(((ClassDefinition) oldref).getAbstractDefinition()).resolveName(n);
      }
      if (ref == null) return null;
    }
    return ref;
  }

}
