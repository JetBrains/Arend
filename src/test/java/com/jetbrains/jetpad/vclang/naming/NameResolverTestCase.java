package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.*;
import com.jetbrains.jetpad.vclang.naming.oneshot.OneshotNameResolver;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.parser.ParserTestCase;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class NameResolverTestCase extends ParserTestCase {
  @SuppressWarnings("StaticNonFinalField")
  private static Abstract.ClassDefinition LOADED_PRELUDE = null;

  private   final SimpleModuleNamespaceProvider  moduleNsProvider  = new SimpleModuleNamespaceProvider();
  protected final SimpleStaticNamespaceProvider  staticNsProvider  = new SimpleStaticNamespaceProvider();
  protected final SimpleDynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();
  private   final NameResolver nameResolver = new NameResolver(moduleNsProvider, staticNsProvider, dynamicNsProvider);

  protected Abstract.ClassDefinition prelude = null;
  private Scope globalScope = new EmptyNamespace();

  protected final void loadPrelude() {
    if (prelude != null) throw new IllegalStateException();

    if (LOADED_PRELUDE == null) {
      ListErrorReporter internalErrorReporter = new ListErrorReporter();
      Prelude.PreludeStorage preludeStorage = new Prelude.PreludeStorage(internalErrorReporter);
      LOADED_PRELUDE = new BaseModuleLoader<>(preludeStorage, new BaseModuleLoader.ModuleLoadingListener()).load(preludeStorage.preludeSourceId).definition;
      assertThat("Failed loading Prelude", internalErrorReporter.getErrorList(), containsErrors(0));

      OneshotNameResolver oneshotNameResolver = new OneshotNameResolver(internalErrorReporter, new ConcreteResolveListener(), moduleNsProvider, staticNsProvider, dynamicNsProvider);
      oneshotNameResolver.visitModule(LOADED_PRELUDE, globalScope);
      assertThat("Failed resolving names in Prelude", internalErrorReporter.getErrorList(), containsErrors(0));
    }

    prelude = LOADED_PRELUDE;
    globalScope = staticNsProvider.forDefinition(prelude);

    moduleNsProvider.registerModule(new ModulePath("Prelude"), prelude);
  }

  protected final void loadModule(ModulePath modulePath, Abstract.ClassDefinition module) {
    ListErrorReporter internalErrorReporter = new ListErrorReporter();
    OneshotNameResolver oneshotNameResolver = new OneshotNameResolver(internalErrorReporter, new ConcreteResolveListener(), moduleNsProvider, staticNsProvider, dynamicNsProvider);
    oneshotNameResolver.visitModule(module, globalScope);
    assertThat("Failed loading helper module", internalErrorReporter.getErrorList(), containsErrors(0));

    moduleNsProvider.registerModule(modulePath, module);
  }


  private Concrete.Expression resolveNamesExpr(Scope parentScope, List<String> context, String text, int errors) {
    Concrete.Expression expression = parseExpr(text);
    assertThat(expression, is(notNullValue()));

    expression.accept(new ExpressionResolveNameVisitor(parentScope, context, nameResolver, errorReporter, new ConcreteResolveListener()), null);
    assertThat(errorList, containsErrors(errors));
    return expression;
  }

  Concrete.Expression resolveNamesExpr(Scope parentScope, String text, int errors) {
    return resolveNamesExpr(parentScope, new ArrayList<String>(), text, errors);
  }

  Concrete.Expression resolveNamesExpr(Scope parentScope, String text) {
    return resolveNamesExpr(parentScope, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(List<Binding> context, String text) {
    List<String> names = new ArrayList<>(context.size());
    for (Binding binding : context) {
      names.add(binding.getName());
    }
    return resolveNamesExpr(globalScope, names, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(new ArrayList<Binding>(), text);
  }


  private void resolveNamesDef(Concrete.Definition definition, int errors) {
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(staticNsProvider, dynamicNsProvider,
        new OverridingScope(globalScope, new SimpleNamespace(definition)), nameResolver, errorReporter, new ConcreteResolveListener());
    definition.accept(visitor, null);
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
