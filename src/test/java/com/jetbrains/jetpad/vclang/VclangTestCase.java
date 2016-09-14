package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.oneshot.OneshotNameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.term.Prelude;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VclangTestCase {
  protected final ListErrorReporter internalErrorReporter = new ListErrorReporter();

  private   final SimpleModuleNamespaceProvider  moduleNsProvider  = new SimpleModuleNamespaceProvider();
  protected final SimpleStaticNamespaceProvider  staticNsProvider  = new SimpleStaticNamespaceProvider();
  protected final SimpleDynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();
  protected final NameResolver nameResolver = new NameResolver(moduleNsProvider, staticNsProvider, dynamicNsProvider);

  protected Abstract.ClassDefinition prelude = null;
  protected Scope globalScope = new EmptyNamespace();

  protected final List<GeneralError> errorList = new ArrayList<>();
  protected final ListErrorReporter errorReporter = new ListErrorReporter(errorList);

  @SafeVarargs
  protected final void assertThatErrorsAre(Matcher<? super GeneralError>... matchers) {
    assertThat(errorList, Matchers.contains(matchers));
  }


  protected final void loadPrelude() {
    if (prelude != null) throw new IllegalStateException();

    prelude = new Prelude.PreludeLoader(internalErrorReporter).load();
    assertThat(internalErrorReporter.getErrorList(), is(empty()));

    globalScope = staticNsProvider.forDefinition(prelude);

    OneshotNameResolver oneshotNameResolver = new OneshotNameResolver(internalErrorReporter, nameResolver, new ConcreteResolveListener(), staticNsProvider, dynamicNsProvider);
    oneshotNameResolver.visitModule(prelude, globalScope);
    assertThat(internalErrorReporter.getErrorList(), is(empty()));

    moduleNsProvider.registerModule(new ModulePath("Prelude"), prelude);
  }

  protected final void loadModule(ModulePath modulePath, Abstract.ClassDefinition module) {
    OneshotNameResolver oneshotNameResolver = new OneshotNameResolver(internalErrorReporter, nameResolver, new ConcreteResolveListener(), staticNsProvider, dynamicNsProvider);
    oneshotNameResolver.visitModule(module, globalScope);
    assertThat(internalErrorReporter.getErrorList(), is(empty()));

    moduleNsProvider.registerModule(modulePath, module);
  }
}
